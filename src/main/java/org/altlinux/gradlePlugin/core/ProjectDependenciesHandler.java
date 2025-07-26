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
package org.altlinux.gradlePlugin.core;

import org.altlinux.gradlePlugin.model.MavenCoordinate;
import org.altlinux.gradlePlugin.services.DefaultPomParser;
import org.altlinux.gradlePlugin.services.FileSystemArtifactVerifier;
import org.altlinux.gradlePlugin.services.PomFinder;

import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.gradle.util.internal.VersionNumber;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.altlinux.gradlePlugin.utils.Painter.*;

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
    private final ScopeManager scopeManager = new ScopeManager();

    private Logger logger;

    private final Map<String, List<String>> bomManagedDeps = new LinkedHashMap<>();

    private final Set<String> initialDeps = new LinkedHashSet<>();
    private final Set<String> transitiveDeps = new LinkedHashSet<>();

    private final Map<String, Set<String>> requestedVersions = new HashMap<>();
    private final Map<String, Set<String>> configurationArtifacts = new HashMap<>();
    private final Map<String, MavenCoordinate> systemArtifacts = new HashMap<>();

    private final Map<String, String> overrideLogs = new ConcurrentHashMap<>();
    private final Map<String, String> applyLogs = new ConcurrentHashMap<>();

    /**
     * Constructs a {@code ProjectDependenciesHandler} with default
     * service implementations for POM parsing and artifact verification.
     */
    public ProjectDependenciesHandler() {
        this.versionScanner = new VersionScanner(
                new PomFinder(new DefaultPomParser()),
                new FileSystemArtifactVerifier()
        );
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
            repositoryManager.addRepository(project.getRepositories());
        });
    }

    /**
     * Executes the full dependency resolution process after project configuration is complete.
     * <p>
     * This includes:
     * <ul>
     *   <li>Collecting declared dependencies</li>
     *   <li>Resolving BOM-managed dependencies</li>
     *   <li>Resolving system-installed versions</li>
     *   <li>Resolving transitive dependencies</li>
     *   <li>Applying substitutions</li>
     *   <li>Attaching resolved artifacts to configurations</li>
     * </ul>
     *
     * @param gradle the current Gradle instance
     */
    public void handleAfterConfiguration(Gradle gradle) {
        Project rootProject = gradle.getRootProject();
        if (logger == null) logger = rootProject.getLogger();

        logger.lifecycle(cyan("===== APPLYING SYSTEM DEPENDENCY VERSIONS ====="));
        logSection("Initial dependencies");

        Set<String> projectDependencies = collectDependencies(gradle);
        initialDeps.addAll(projectDependencies);
        logger.lifecycle("Found {} dependencies", projectDependencies.size());

        BomDependencyManager bomManager = new BomDependencyManager(
                new PomFinder(new DefaultPomParser()), logger);
        Set<String> allDependencies = bomManager.processBomDependencies(projectDependencies);
        this.bomManagedDeps.putAll(bomManager.getBomManagedDeps());
        removeBomDependencies(gradle, bomManager.getProcessedBoms());

        logSection("Dependencies managed by BOM");
        bomManager.getBomManagedDeps().forEach((bom, deps) -> {
            logger.lifecycle("BOM: {} ({} dependencies)", cyan(bom), deps.size());
        });

        logSection("Resolved system artifacts");
        systemArtifacts.putAll(versionScanner.scanSystemArtifacts(allDependencies, logger));

        filterTestDependencies();
        filterBomDependencies();

        logger.lifecycle("Resolved {} artifacts", systemArtifacts.size());
        logger.info("System artifacts:");
        systemArtifacts.forEach((key, coord) -> logger.info(" - {}:{}:{}",
                coord.groupId, coord.artifactId, coord.version));

        TransitiveDependencyManager transitiveManager = new TransitiveDependencyManager(
                new PomFinder(new DefaultPomParser()),
                new FileSystemArtifactVerifier(),
                logger
        );

        Set<String> transitiveDepsSet = transitiveManager.processTransitiveDependencies(
                new HashSet<>(systemArtifacts.keySet()),
                systemArtifacts,
                new HashSet<>(bomManager.getManagedDependencies())
        );
        transitiveDeps.addAll(transitiveDepsSet);

        for (MavenCoordinate coord : transitiveManager.getTransitiveDependencies()) {
            String key = coord.groupId + ":" + coord.artifactId;
            if (coord.scope != null) {
                scopeManager.updateScope(key, coord.scope);
            }
        }

        Set<String> newDependencies = new HashSet<>(transitiveDepsSet);
        newDependencies.removeAll(systemArtifacts.keySet());

        if (!newDependencies.isEmpty()) {
            logSection("New dependencies from transitive closure");
            logger.lifecycle("Found {} new dependencies", newDependencies.size());
            Map<String, MavenCoordinate> transitiveArtifacts = versionScanner.scanSystemArtifacts(newDependencies, logger);
            systemArtifacts.putAll(transitiveArtifacts);
        }

        addSystemArtifactsToConfigurations(gradle, systemArtifacts);
        applySubstitutions(gradle, systemArtifacts);

        logger.lifecycle(cyan("===== DEPENDENCY RESOLUTION COMPLETED ====="));
        logSection("Added artifacts to configurations");
        logConfigurationArtifacts();

        Set<String> notFoundDeps = versionScanner.getNotFoundDependencies();
        Set<String> skippedDeps = transitiveManager.getSkippedDependencies();

        if (!notFoundDeps.isEmpty() || !skippedDeps.isEmpty()) {
            logSection("Skipped dependencies");
            notFoundDeps.forEach(dep -> logger.lifecycle(yellow("Not found: {}", dep)));
            skippedDeps.forEach(dep -> logger.lifecycle(yellow("Skipped: {}", dep)));
        }

        gradle.getTaskGraph().whenReady(taskGraph -> {
            logSection("Dependency substitutions");
            logSubstitutions();
        });
    }

    /**
     * Applies version substitutions for all supported configurations based on resolved system artifacts.
     *
     * @param gradle the Gradle instance
     * @param systemArtifacts the map of resolved artifacts to substitute
     */
    private void applySubstitutions(Gradle gradle, Map<String, MavenCoordinate> systemArtifacts) {
        gradle.allprojects(project -> {
            project.getConfigurations().matching(config ->
                    config.getName().endsWith("Implementation") ||
                            "compileClasspath".equals(config.getName())
            ).all(config -> {
                config.getResolutionStrategy()
                        .dependencySubstitution(substitutions -> {
                            substitutions.all(details -> {
                                if (!(details.getRequested() instanceof ModuleComponentSelector)) {
                                    return;
                                }
                                ModuleComponentSelector sel = (ModuleComponentSelector) details.getRequested();
                                String key = sel.getGroup() + ":" + sel.getModule();
                                MavenCoordinate sys = systemArtifacts.get(key);
                                if (sys == null || sys.isBom()) {
                                    return;
                                }

                                details.useTarget(
                                        substitutions.module(key + ":" + sys.version),
                                        "System dependency override"
                                );

                                Set<String> originalVersions = requestedVersions.get(key);
                                String originalVersion = originalVersions != null && !originalVersions.isEmpty() ?
                                        originalVersions.stream()
                                                .filter(Objects::nonNull)
                                                .max(Comparator.comparing(VersionNumber::parse))
                                                .orElse(null) : null;

                                if (originalVersion == null) {
                                    originalVersion = sel.getVersion();
                                    if (originalVersion == null || originalVersion.isEmpty()) {
                                        originalVersion = "(unspecified)";
                                    }
                                }

                                String logKey = key + ":" + sys.version;
                                if (sys.version.equals(originalVersion)) {
                                    applyLogs.putIfAbsent(logKey,
                                            String.format("Apply version: %s:%s", key, sys.version));
                                } else {
                                    overrideLogs.putIfAbsent(logKey,
                                            String.format("Override version: %s:%s -> %s",
                                                    key, originalVersion, sys.version));
                                }
                            });
                        });
            });
        });
    }

    /**
     * Logs all dependency substitutions (both applied and overridden versions).
     */
    private void logSubstitutions() {
        if (!overrideLogs.isEmpty()) {
            logger.lifecycle(green("Overridden versions:"));
            overrideLogs.values().stream()
                    .sorted()
                    .forEach(logger::lifecycle);
        }

        if (!applyLogs.isEmpty()) {
            logger.lifecycle(green("\nApplied versions:"));
            applyLogs.values().stream()
                    .sorted()
                    .forEach(logger::lifecycle);
        }
    }

    /**
     * Removes dependencies with scope "test" from the set of resolved system artifacts.
     * <p>
     * This prevents test-scoped dependencies from being added to production configurations.
     */
    private void filterTestDependencies() {
        systemArtifacts.entrySet().removeIf(e -> "test".equals(e.getValue().scope));
    }

    /**
     * Removes BOM (Bill of Materials) entries from the set of resolved system artifacts.
     * <p>
     * BOMs are used only for version management and should not be added as actual dependencies.
     */
    private void filterBomDependencies() {
        systemArtifacts.entrySet().removeIf(e -> e.getValue().isBom());
    }

    /**
     * Logs a section header in the build output to visually separate log entries.
     *
     * @param title the title of the log section
     */
    private void logSection(String title) {
        logger.lifecycle(green("\n--- " + title + " ---"));
    }

    /**
     * Logs all resolved artifacts grouped by their assigned configuration.
     * <p>
     * This includes information about which system artifacts were added to
     * each configuration (e.g., implementation, runtimeOnly, etc.).
     */
    private void logConfigurationArtifacts() {
        configurationArtifacts.forEach((cfg, arts) -> {
            logger.lifecycle("{} ({} artifacts):", cyan(cfg), arts.size());
            arts.forEach(a -> logger.lifecycle(" - {}", a));
        });
    }

    /**
     * Adds resolved system artifacts to the appropriate Gradle configurations
     * (e.g. implementation, compileOnly, runtimeOnly).
     *
     * @param gradle the Gradle instance
     * @param sysArts resolved system artifact coordinates
     */
    private void addSystemArtifactsToConfigurations(Gradle gradle, Map<String, MavenCoordinate> sysArts) {
        gradle.allprojects(proj -> {
            configurationArtifacts.clear();
            sysArts.forEach((key, coord) -> {
                if (coord.isBom() || "pom".equals(coord.packaging)) return;
                String notation = key + ":" + coord.version;
                String scope = scopeManager.getScope(key);
                String cfgName;
                if ("provided".equals(scope)) {
                    proj.getDependencies().add("compileOnly", notation);
                    proj.getDependencies().add("testImplementation", notation);
                    cfgName = "compileOnly/testImplementation";
                } else if ("runtime".equals(scope)) {
                    proj.getDependencies().add("runtimeOnly", notation);
                    cfgName = "runtimeOnly";
                } else {
                    proj.getDependencies().add("implementation", notation);
                    cfgName = "implementation";
                }
                configurationArtifacts
                        .computeIfAbsent(cfgName, k -> new LinkedHashSet<>())
                        .add(notation);
            });
        });
    }

    /**
     * Collects all dependencies declared in all configurations across all projects.
     *
     * @param gradle the Gradle instance
     * @return a set of group:artifact keys for declared dependencies
     */
    private Set<String> collectDependencies(Gradle gradle) {
        Set<String> set = new LinkedHashSet<>();
        gradle.allprojects(p -> {
            p.getConfigurations().all(cfg -> {
                for (Dependency d : cfg.getDependencies()) {
                    if (d.getGroup() != null && d.getName() != null) {
                        String key = d.getGroup() + ":" + d.getName();
                        set.add(key);
                        requestedVersions
                                .computeIfAbsent(key, k -> new HashSet<>())
                                .add(d.getVersion());
                    }
                }
            });
        });
        return set;
    }

    /**
     * Removes dependencies that are managed by BOM from all configurations.
     *
     * @param gradle the Gradle instance
     * @param bomKeys the keys of BOM-managed dependencies to remove
     */
    private void removeBomDependencies(Gradle gradle, Set<String> bomKeys) {
        gradle.allprojects(p -> {
            p.getConfigurations().all(cfg -> {
                List<Dependency> toRemove = new ArrayList<>();
                for (Dependency d : cfg.getDependencies()) {
                    String key = d.getGroup() + ":" + d.getName();
                    if (bomKeys.contains(key)) toRemove.add(d);
                }
                toRemove.forEach(cfg.getDependencies()::remove);
            });
        });
    }
}