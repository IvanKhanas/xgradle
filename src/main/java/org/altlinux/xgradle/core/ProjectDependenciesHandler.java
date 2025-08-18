/*
 * Copyright 2025 BaseALT Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.altlinux.xgradle.core;

import org.altlinux.xgradle.core.collectors.ConfigurationInfoCollector;
import org.altlinux.xgradle.core.collectors.DefaultDependencyCollector;
import org.altlinux.xgradle.core.collectors.info.ConfigurationInfo;
import org.altlinux.xgradle.core.configurators.DefaultArtifactConfigurator;
import org.altlinux.xgradle.core.managers.RepositoryManager;
import org.altlinux.xgradle.core.processors.BomProcessor;
import org.altlinux.xgradle.core.processors.TransitiveProcessor;
import org.altlinux.xgradle.core.resolvers.ArtifactResolver;
import org.altlinux.xgradle.model.MavenCoordinate;
import org.altlinux.xgradle.services.DefaultPomParser;
import org.altlinux.xgradle.services.FileSystemArtifactVerifier;
import org.altlinux.xgradle.services.PomFinder;
import org.altlinux.xgradle.services.VersionScanner;
import org.altlinux.xgradle.utils.loggers.DependencyLogger;

import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;

import java.util.*;

/**
 * Handles dependency resolution and substitution for a Gradle project using system-installed artifacts.
 * <p>
 * This class orchestrates:
 * <ul>
 *   <li>Collecting declared project dependencies</li>
 *   <li>Managing dependencies declared in BOM (Bill of Materials)</li>
 *   <li>Resolving system-installed versions using POM metadata</li>
 *   <li>Performing transitive dependency resolution</li>
 *   <li>Applying dependency substitutions</li>
 *   <li>Adding resolved artifacts to appropriate Gradle configurations</li>
 * </ul>
 *
 * It is intended to be used as part of a custom Gradle plugin to enforce controlled,
 * reproducible dependency resolution based on a predefined set of artifacts available
 * on the local file system.
 *
 * @author Ivan Khanas
 */
public class ProjectDependenciesHandler {
    private final RepositoryManager repositoryManager;
    private final VersionScanner versionScanner;
    private Logger logger;

    /**
     * Constructs a {@code ProjectDependenciesHandler} with default
     * service implementations for POM parsing and artifact verification.
     */
    public ProjectDependenciesHandler() {
        this.versionScanner = new VersionScanner(new PomFinder(new DefaultPomParser()), new FileSystemArtifactVerifier());
        this.repositoryManager = new RepositoryManager(null);
    }

    /**
     * Adds the system repository to all projects in the Gradle build.
     *
     * @param gradle the current Gradle instance
     */
    public void addRepository(Gradle gradle) {
        gradle.allprojects(project -> {
            if (logger == null) logger = project.getLogger();
            repositoryManager.setLogger(logger);
            repositoryManager.configureDependenciesRepository(project.getRepositories());
        });
    }

    /**
     * Executes full dependency resolution after project configuration.
     *
     * <p>Key steps:
     * <ol>
     *   <li>Collect declared dependencies and configuration metadata</li>
     *   <li>Process BOMs to expand managed dependencies</li>
     *   <li>Resolve system-installed artifacts</li>
     *   <li>Identify test-scoped dependencies</li>
     *   <li>Resolve transitive dependencies (main/test)</li>
     *   <li>Configure resolved artifacts in Gradle</li>
     *   <li>Apply dependency version substitutions</li>
     * </ol>
     *
     * <p>Produces detailed logs for each resolution stage and reports:
     * <ul>
     *   <li>Resolved artifacts</li>
     *   <li>Dependency substitutions</li>
     *   <li>Skipped/unresolved dependencies</li>
     * </ul>
     *
     * @param gradle current Gradle build instance
     */
    public void handleAfterConfiguration(Gradle gradle) {
        Project rootProject = gradle.getRootProject();
        if (logger == null) logger = rootProject.getLogger();
        DependencyLogger depLogger = new DependencyLogger();
        depLogger.logSection("\n===== APPLYING SYSTEM DEPENDENCY VERSIONS =====", logger);
        depLogger.logSection("Initial dependencies", logger);

        DefaultDependencyCollector dependencyCollector = new DefaultDependencyCollector();
        Set<String> projectDeps = dependencyCollector.collect(gradle);
        Map<String, Set<String>> requestedVersions = dependencyCollector.getRequestedVersions();
        depLogger.logInitialDependencies(projectDeps, logger);

        ConfigurationInfoCollector configurationCollector = new ConfigurationInfoCollector();
        configurationCollector.collect(gradle);

        Map<String, Boolean> testDependencyFlags = configurationCollector.getTestDependencyFlags();
        Map<String, Set<ConfigurationInfo>> dependencyConfigurations = configurationCollector.getDependencyConfigurations();
        Map<String, Set<String>> dependencyConfigNames = configurationCollector.getDependencyConfigNames();

        BomProcessor bomProcessor = new BomProcessor();
        Set<String> allDeps = bomProcessor.process(projectDeps, new PomFinder(new DefaultPomParser()), logger);
        bomProcessor.removeBomsFromConfigurations(gradle);

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
                dependencyConfigNames
        );

        DependencySubstitutor substitutor = new DependencySubstitutor(
                requestedVersions,
                artifactResolver.getSystemArtifacts(),
                bomProcessor.getManagedVersions()
        );

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