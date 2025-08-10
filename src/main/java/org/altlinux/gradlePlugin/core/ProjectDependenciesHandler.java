package org.altlinux.gradlePlugin.core;

import org.altlinux.gradlePlugin.core.collectors.ConfigurationInfoCollector;
import org.altlinux.gradlePlugin.core.collectors.DefaultDependencyCollector;
import org.altlinux.gradlePlugin.core.collectors.info.ConfigurationInfo;
import org.altlinux.gradlePlugin.core.configurators.DefaultArtifactConfigurator;
import org.altlinux.gradlePlugin.core.managers.RepositoryManager;
import org.altlinux.gradlePlugin.core.processors.BomProcessor;
import org.altlinux.gradlePlugin.core.processors.TransitiveProcessor;
import org.altlinux.gradlePlugin.core.resolvers.ArtifactResolver;
import org.altlinux.gradlePlugin.model.MavenCoordinate;
import org.altlinux.gradlePlugin.services.DefaultPomParser;
import org.altlinux.gradlePlugin.services.FileSystemArtifactVerifier;
import org.altlinux.gradlePlugin.services.PomFinder;
import org.altlinux.gradlePlugin.services.VersionScanner;
import org.altlinux.gradlePlugin.utils.loggers.DependencyLogger;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import java.util.*;

public class ProjectDependenciesHandler {
    private final RepositoryManager repositoryManager;
    private final VersionScanner versionScanner;
    private Logger logger;

    public ProjectDependenciesHandler() {
        this.versionScanner = new VersionScanner(new PomFinder(new DefaultPomParser()), new FileSystemArtifactVerifier());
        this.repositoryManager = new RepositoryManager(null);
    }

    public void addRepository(Gradle gradle) {
        gradle.allprojects(project -> {
            if (logger == null) logger = project.getLogger();
            repositoryManager.setLogger(logger);
            repositoryManager.configureDependenciesRepository(project.getRepositories());
        });
    }

    public void handleAfterConfiguration(Gradle gradle) {
        Project rootProject = gradle.getRootProject();
        if (logger == null) logger = rootProject.getLogger();
        DependencyLogger depLogger = new DependencyLogger();
        depLogger.logSection("\n===== APPLYING SYSTEM DEPENDENCY VERSIONS =====", logger);
        depLogger.logSection("Initial dependencies", logger);

        ConfigurationInfoCollector configurationCollector = new ConfigurationInfoCollector();
        configurationCollector.collect(gradle);
        Map<String, Boolean> testDependencyFlags = configurationCollector.getTestDependencyFlags();
        Map<String, Set<ConfigurationInfo>> dependencyConfigurations = configurationCollector.getDependencyConfigurations();
        Map<String, Set<String>> dependencyConfigNames = configurationCollector.getDependencyConfigNames();

        DefaultDependencyCollector dependencyCollector = new DefaultDependencyCollector();
        Set<String> projectDeps = dependencyCollector.collect(gradle);
        Map<String, Set<String>> requestedVersions = dependencyCollector.getRequestedVersions();
        depLogger.logInitialDependencies(projectDeps, logger);

        BomProcessor bomProcessor = new BomProcessor(testDependencyFlags);
        Set<String> allDeps = bomProcessor.process(projectDeps, new PomFinder(new DefaultPomParser()), logger);
        bomProcessor.removeBomsFromConfigurations(gradle);
        depLogger.logSection("Dependencies managed by BOM", logger);
        depLogger.logBomDependencies(bomProcessor.getBomManagedDeps(), logger);

        ArtifactResolver artifactResolver = new ArtifactResolver(versionScanner);
        artifactResolver.resolve(allDeps, logger);
        artifactResolver.filter();
        depLogger.logSection("Resolved system artifacts", logger);
        depLogger.logResolvedArtifacts(artifactResolver.getSystemArtifacts(), logger);

        Set<String> testContextDependencies = new HashSet<>();
        for (Map.Entry<String, Boolean> entry : testDependencyFlags.entrySet()) {
            if (entry.getValue()) {
                testContextDependencies.add(entry.getKey());
            }
        }

        bomProcessor.getBomManagedDeps().forEach((bomKey, deps) -> {
            String[] parts = bomKey.split(":");
            if (parts.length >= 2) {
                String bomId = parts[0] + ":" + parts[1];
                if (testDependencyFlags.getOrDefault(bomId, false)) {
                    deps.forEach(dep -> {
                        String[] depParts = dep.split(":");
                        if (depParts.length >= 2) {
                            testContextDependencies.add(depParts[0] + ":" + depParts[1]);
                        }
                    });
                }
            }
        });

        TransitiveProcessor transitiveProcessor = new TransitiveProcessor(
                new PomFinder(new DefaultPomParser()),
                logger,
                testContextDependencies
        );
        transitiveProcessor.process(artifactResolver.getSystemArtifacts());
        testContextDependencies.addAll(transitiveProcessor.getTestDependencies());

        depLogger.logSection("Test context dependencies", logger);
        depLogger.logTestContextDependencies(testContextDependencies, logger);

        Set<String> mainDeps = transitiveProcessor.getMainDependencies();
        Set<String> newMainDeps = new HashSet<>(mainDeps);
        newMainDeps.removeAll(artifactResolver.getSystemArtifacts().keySet());

        if (!newMainDeps.isEmpty()) {
            depLogger.logSection("New main dependencies from transitive closure", logger);
            depLogger.logNewDependencies(newMainDeps, logger);
            Map<String, MavenCoordinate> mainArtifacts = versionScanner.scanSystemArtifacts(newMainDeps, logger);
            artifactResolver.getSystemArtifacts().putAll(mainArtifacts);
        }

        Set<String> testDeps = transitiveProcessor.getTestDependencies();
        Set<String> newTestDeps = new HashSet<>(testDeps);
        newTestDeps.removeAll(artifactResolver.getSystemArtifacts().keySet());

        if (!newTestDeps.isEmpty()) {
            depLogger.logSection("New test dependencies from transitive closure", logger);
            depLogger.logNewDependencies(newTestDeps, logger);
            Map<String, MavenCoordinate> testArtifacts = versionScanner.scanSystemArtifacts(newTestDeps, logger);
            artifactResolver.getSystemArtifacts().putAll(testArtifacts);
        }

        DefaultArtifactConfigurator configurator = new DefaultArtifactConfigurator(
                transitiveProcessor.getScopeManager(),
                dependencyConfigurations,
                testContextDependencies
        );
        configurator.configure(gradle,
                artifactResolver.getSystemArtifacts(),
                dependencyConfigNames);

        DependencySubstitutor substitutor = new DependencySubstitutor(requestedVersions, artifactResolver.getSystemArtifacts());
        substitutor.configure(gradle);

        depLogger.logSection("===== DEPENDENCY RESOLUTION COMPLETED =====", logger);
        depLogger.logSection("Added artifacts to configurations", logger);
        depLogger.logConfigurationArtifacts(configurator.getConfigurationArtifacts(), logger);

        Set<String> notFound = artifactResolver.getNotFoundDependencies();
        Set<String> skipped = transitiveProcessor.getSkippedDependencies();
        if (!notFound.isEmpty() || !skipped.isEmpty()) {
            depLogger.logSection("Skipped dependencies", logger);
            depLogger.logSkippedDependencies(notFound, skipped, logger);
        }

        gradle.getTaskGraph().whenReady(taskGraph -> {
            depLogger.logSection("Dependency substitutions", logger);
            depLogger.logSubstitutions(substitutor.getOverrideLogs(), substitutor.getApplyLogs(), logger);
        });
    }
}