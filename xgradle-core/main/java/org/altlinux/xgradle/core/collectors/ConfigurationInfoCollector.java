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
package org.altlinux.xgradle.core.collectors;

import org.altlinux.xgradle.core.collectors.info.ConfigurationInfo;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.invocation.Gradle;

import java.util.*;


/**
 * Collects and organizes dependency configuration information across all projects in a Gradle build.
 *
 * <p>This collector traverses all configurations in all projects, gathering metadata about
 * how dependencies are used in different configurations. It tracks three key aspects for each dependency:
 * <ul>
 *   <li>Configuration metadata objects ({@link ConfigurationInfo})</li>
 *   <li>Test dependency flags indicating if a dependency is used in any test configuration</li>
 *   <li>Names of configurations where each dependency appears</li>
 * </ul>
 *
 * <p>The collected information is organized by dependency coordinates (group:name) and can be
 * accessed through the provided getter methods after collection.
 *
 * <p>Main features:
 * <ul>
 *   <li>Aggregates configuration metadata across entire build</li>
 *   <li>Identifies test dependencies based on configuration usage</li>
 *   <li>Provides quick lookup of configuration names per dependency</li>
 * </ul>
 *
 * @see ConfigurationInfo
 *
 * @author Ivan Khanas
 */
public class ConfigurationInfoCollector {
    private final Map<String, Set<ConfigurationInfo>> dependencyConfigurations = new HashMap<>();
    private final Map<String, Boolean> testDependencyFlags = new HashMap<>();
    private final Map<String, Set<String>> dependencyConfigNames = new HashMap<>();

    /**
     * Collects dependency configuration information from all projects in the Gradle build.
     *
     * <p>This method traverses all configurations of all projects, processing each dependency
     * to gather:
     * <ul>
     *   <li>Configuration metadata objects</li>
     *   <li>Test configuration flags</li>
     *   <li>Configuration names where dependency appears</li>
     * </ul>
     *
     * <p>Dependencies are indexed by their coordinates (group:name). For test dependencies,
     * the collector marks a dependency as test-imiterere if it appears in any test configuration,
     * while preserving non-test status if it appears in both test and non-test configurations.
     *
     * @param gradle The Gradle instance representing the build
     */
    public void collect(Gradle gradle) {
        gradle.allprojects(project ->
                project.getConfigurations().all(configuration -> {
                    ConfigurationInfo configInfo = new ConfigurationInfo(configuration);
                    for (Dependency dependency : configuration.getDependencies()) {
                        if (dependency.getGroup() != null && dependency.getName() != null) {
                            String key = dependency.getGroup() + ":" + dependency.getName();

                            dependencyConfigurations
                                    .computeIfAbsent(key, k -> new HashSet<>())
                                    .add(configInfo);

                            dependencyConfigNames
                                    .computeIfAbsent(key, k -> new HashSet<>())
                                    .add(configuration.getName());

                            if (configInfo.isTestConfigutation()) {
                                testDependencyFlags.put(key, true);
                            } else {
                                testDependencyFlags.putIfAbsent(key, false);
                            }
                        }
                    }
                })
        );
    }

    /**
     * Returns collected configuration metadata for dependencies.
     *
     * <p>The returned map uses dependency coordinates (group:name) as keys, with values
     * being sets of {@link ConfigurationInfo} objects representing the different
     * configurations where each dependency is used.
     *
     * @return Map of dependency coordinates to their configuration metadata sets
     */
    public Map<String, Set<ConfigurationInfo>> getDependencyConfigurations() {
        return dependencyConfigurations;
    }

    /**
     * Returns test dependency flags for collected dependencies.
     *
     * <p>The returned map indicates for each dependency (keyed by group:name) whether
     * it is used in any test configuration. A value of {@code true} means the dependency
     * appears in at least one test configuration, while {@code false} means it only
     * appears in non-test configurations.
     *
     * @return Map of dependency coordinates to their test dependency status
     */
    public Map<String, Boolean> getTestDependencyFlags() {
        return testDependencyFlags;
    }

    /**
     * Returns configuration names where dependencies appear.
     *
     * <p>The returned map provides for each dependency (keyed by group:name) the set
     * of configuration names where the dependency is declared.
     *
     * @return Map of dependency coordinates to sets of configuration names
     */
    public Map<String, Set<String>> getDependencyConfigNames() {
        return dependencyConfigNames;
    }
}