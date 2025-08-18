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
package org.altlinux.gradlePlugin.core.configurators;

import org.altlinux.gradlePlugin.api.ArtifactConfigurator;
import org.altlinux.gradlePlugin.core.collectors.info.ConfigurationInfo;
import org.altlinux.gradlePlugin.core.managers.ScopeManager;
import org.altlinux.gradlePlugin.model.MavenCoordinate;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.invocation.Gradle;

import java.util.*;

/**
 * Configures artifacts and dependencies in Gradle projects based on various criteria.
 *
 * <p>This class implements the {@link ArtifactConfigurator} interface to manage artifact
 * configuration across all projects in a Gradle build. It handles:</p>
 * <ul>
 *   <li>Filtering out BOM and POM artifacts</li>
 *   <li>Adding artifacts to explicitly specified configurations</li>
 *   <li>Automatically assigning artifacts to appropriate configurations (api, implementation, etc.)
 *       based on scope and dependency type</li>
 *   <li>Special handling for test-context dependencies</li>
 * </ul>
 *
 * <p>The configurator tracks all added artifacts per configuration through the
 * {@link #getConfigurationArtifacts()} method.</p>
 *
 * @author Ivan Khanas
 */
public class DefaultArtifactConfigurator implements ArtifactConfigurator {
    private final ScopeManager scopeManager;
    private final Map<String, Set<String>> configurationArtifacts = new HashMap<>();
    private final Map<String, Set<ConfigurationInfo>> dependencyConfigurations;
    private final Set<String> testContextDependencies;

    /**
     * Constructs a DefaultArtifactConfigurator with necessary dependency managers.
     *
     * @param scopeManager manager for resolving dependency scopes
     * @param dependencyConfigurations mapping of dependency keys to their configuration metadata
     * @param testContextDependencies set of dependency keys marked for test context
     */
    public DefaultArtifactConfigurator(
            ScopeManager scopeManager,
            Map<String, Set<ConfigurationInfo>> dependencyConfigurations,
            Set<String> testContextDependencies) {
        this.scopeManager = scopeManager;
        this.dependencyConfigurations = dependencyConfigurations;
        this.testContextDependencies = testContextDependencies; }

    /**
     * Configures artifacts across all Gradle projects.
     *
     * <p>Processes system artifacts and adds them to appropriate configurations
     * based on predefined rules and metadata. This method:</p>
     * <ul>
     *   <li>Skips BOM and POM-type artifacts</li>
     *   <li>Uses explicitly defined configurations when available</li>
     *   <li>Automatically determines configurations for other artifacts</li>
     *   <li>Tracks all added artifacts in {@link #configurationArtifacts}</li>
     * </ul>
     *
     * @param gradle Gradle instance for project access
     * @param systemArtifacts map of system artifacts to configure
     * @param dependencyConfigNames mapping of dependency keys to configuration names
     */
    @Override
    public void configure(Gradle gradle,
                          Map<String, MavenCoordinate> systemArtifacts,
                          Map<String, Set<String>> dependencyConfigNames) {

        gradle.allprojects(proj -> {
            configurationArtifacts.clear();
            systemArtifacts.forEach((key, coord) -> {
                if (shouldSkip(coord)) return;

                Set<String> configNames = dependencyConfigNames.get(key);
                if (configNames != null && !configNames.isEmpty()) {
                    addToOriginalConfigurations(proj, key, coord, configNames);
                } else {
                    addBasedOnScope(proj, key, coord);
                }
            });
        });
    }

    /**
     * Determines whether the given artifact should be skipped during configuration.
     *
     * <p>An artifact is excluded if it represents a BOM (Bill of Materials) or has
     * packaging type {@code pom}. Such artifacts typically do not contain any
     * actual binaries and are not directly consumable by Gradle configurations.</p>
     *
     * @param coord the Maven coordinate of the artifact
     *
     * @return {@code true} if the artifact should not be added to any configuration,
     *         {@code false} otherwise
     */
    private boolean shouldSkip(MavenCoordinate coord) {
        return coord.isBom() || "pom".equals(coord.getPackaging());
    }

    /**
     * Adds an artifact to its originally specified configurations.
     *
     * <p>If explicit configuration names are provided for a dependency (for example,
     * from metadata or user configuration), the artifact is added only to those
     * configurations. Each successful addition is also tracked in the
     * {@link #configurationArtifacts} map for later retrieval.</p>
     *
     * @param project     the current Gradle project
     * @param key         the dependency key (usually {@code groupId:artifactId})
     * @param coord       the Maven coordinate of the artifact
     * @param configNames the set of configuration names to which the dependency
     *                    should be explicitly added
     */
    private void addToOriginalConfigurations(Project project,
                                             String key,
                                             MavenCoordinate coord,
                                             Set<String> configNames) {

        String notation = key + ":" + coord.getVersion();
        for (String configName : configNames) {
            Configuration config = project.getConfigurations().findByName(configName);
            if (config != null) {
                project.getDependencies().add(configName, notation);
                trackArtifact(configName, notation);
            }
        }
    }

    /**
     * Adds an artifact to appropriate configurations based on dependency scope and type.
     *
     * <p>This method first checks whether the dependency is test-related
     * (either present in {@link #testContextDependencies} or marked as a test
     * context in its metadata). In such cases, it is always added to
     * {@code testImplementation}.
     *
     * <p>If not test-related, the method attempts to determine the configuration
     * type using {@link #determineConfigurationType(String)}. Depending on the
     * result, the artifact is added to {@code api}, {@code implementation},
     * {@code runtimeOnly}, {@code compileOnly}, or {@code testImplementation}.
     *
     * <p>If no explicit type is found, a fallback resolution is performed using
     * {@link #addBasedOnScopeDefault(Project, String, String)} which relies on
     * the dependency scope provided by {@link ScopeManager}.</p>
     *
     * @param project the current Gradle project
     * @param key     the dependency key
     * @param coord   the Maven coordinate of the artifact
     */
    private void addBasedOnScope(Project project, String key, MavenCoordinate coord) {
        String notation = key + ":" + coord.getVersion();

        if (testContextDependencies.contains(key) || coord.isTestContext()) {
            addToTestConfiguration(project, notation);
            return;
        }

        String type = determineConfigurationType(key);

        if (type != null) {
            switch (type) {
                case "API":
                    project.getDependencies().add("api", notation);
                    trackArtifact("api", notation);
                    break;
                case "IMPLEMENTATION":
                    project.getDependencies().add("implementation", notation);
                    trackArtifact("implementation", notation);
                    break;
                case "RUNTIME":
                    project.getDependencies().add("runtimeOnly", notation);
                    trackArtifact("runtimeOnly", notation);
                    break;
                case "COMPILE_ONLY":
                    project.getDependencies().add("compileOnly", notation);
                    trackArtifact("compileOnly", notation);
                case "TEST":
                    addToTestConfiguration(project, notation);
                    break;
                default:
                    addBasedOnScopeDefault(project, key, notation);
            }
        } else {
            addBasedOnScopeDefault(project, key, notation);
        }
    }

    /**
     * Determines the configuration type for a dependency key.
     *
     * <p>If metadata about the dependency is available in {@link #dependencyConfigurations},
     * the method inspects all associated {@link ConfigurationInfo} objects. It returns
     * the type of the first configuration that is <strong>not</strong> marked as a test
     * configuration. If all configurations are test-related or none are available,
     * this method returns {@code null}.</p>
     *
     * @param key the dependency key
     *
     * @return the configuration type (e.g., {@code "API"}, {@code "IMPLEMENTATION"},
     *         {@code "RUNTIME"}, {@code "COMPILE_ONLY"}), or {@code null} if it cannot
     *         be determined
     */
    private String determineConfigurationType(String key) {
        if (dependencyConfigurations.containsKey(key)) {
            for (ConfigurationInfo configInfo : dependencyConfigurations.get(key)) {
                if (!configInfo.isTestConfigutation()) {
                    return configInfo.getType();
                }
            }
        }
        return null;
    }

    /**
     * Adds an artifact to the Gradle test configuration.
     *
     * <p>All test-related dependencies are consistently placed into the
     * {@code testImplementation} configuration. Each addition is also tracked
     * for reporting purposes in {@link #configurationArtifacts}.</p>
     *
     * @param project  the current Gradle project
     * @param notation the artifact in Gradle notation format
     *                 ({@code group:artifact:version})
     */
    private void addToTestConfiguration(Project project, String notation) {
        project.getDependencies().add("testImplementation", notation);
        trackArtifact("testImplementation", notation);
    }

    /**
     * Adds an artifact to a configuration based on its resolved scope (fallback strategy).
     *
     * <p>This method is used when no explicit configuration type is available
     * from metadata. It uses {@link ScopeManager} to determine the effective scope
     * of the dependency and maps it to the closest Gradle configuration:</p>
     *
     * <ul>
     *   <li>{@code provided} or {@code compileOnly} → {@code compileOnly}</li>
     *   <li>{@code runtime} or {@code runtimeOnly} → {@code runtimeOnly}</li>
     *   <li>All other scopes → {@code implementation}</li>
     * </ul>
     *
     * @param project  the current Gradle project
     * @param key      the dependency key
     * @param notation the artifact in Gradle notation format
     *                 ({@code group:artifact:version})
     */
    private void addBasedOnScopeDefault(Project project, String key, String notation) {
        String scope = scopeManager.getScope(key);
        if ("provided".equals(scope) || "compileOnly".equals(scope)) {
            project.getDependencies().add("compileOnly", notation);
            trackArtifact("compileOnly", notation);
        } else if ("runtime".equals(scope) || "runtimeOnly".equals(scope)) {
            project.getDependencies().add("runtimeOnly", notation);
            trackArtifact("runtimeOnly", notation);
        } else {
            project.getDependencies().add("implementation", notation);
            trackArtifact("implementation", notation);
        }
    }

    /**
     * Records the fact that an artifact was added to a specific configuration.
     *
     * <p>This method updates the internal {@link #configurationArtifacts} map,
     * ensuring that the artifact can later be retrieved and reported by
     * configuration. Duplicate entries are avoided by using a {@link LinkedHashSet}.</p>
     *
     * @param config   the name of the configuration (e.g., {@code implementation}, {@code api})
     * @param artifact the artifact in Gradle notation format
     *                 ({@code group:artifact:version})
     */
    private void trackArtifact(String config, String artifact) {
        configurationArtifacts
                .computeIfAbsent(config, k -> new LinkedHashSet<>())
                .add(artifact);
    }

    /**
     * Returns a mapping of configurations to the artifacts that were added to them.
     *
     * <p>The returned map contains configuration names as keys (such as
     * {@code implementation}, {@code api}, {@code runtimeOnly}, etc.) and
     * the set of artifact coordinates assigned to each configuration.
     * This provides a complete overview of how artifacts have been distributed
     * across the Gradle project’s configurations.</p>
     *
     * @return a map of configuration names to sets of artifact notations
     */
    public Map<String, Set<String>> getConfigurationArtifacts() {
        return configurationArtifacts;
    }
}   
