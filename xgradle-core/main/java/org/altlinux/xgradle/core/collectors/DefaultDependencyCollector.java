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

import org.altlinux.xgradle.api.DependencyCollector;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.invocation.Gradle;

import java.util.*;

/**
 * Default implementation of {@link DependencyCollector} that inspects
 * all projects in a Gradle build and gathers declared dependencies.
 *
 * <p>This collector traverses every project and every configuration within
 * those projects, aggregating information about all explicitly declared
 * dependencies. For each dependency, both its {@code group:artifact} key
 * and all requested versions are stored.</p>
 *
 * <p>The collected data serves two purposes:</p>
 * <ul>
 *   <li>Providing a deduplicated set of dependency keys across the build</li>
 *   <li>Tracking all versions requested for each dependency key, which can
 *       later be used for version alignment, conflict resolution, or reporting</li>
 * </ul>
 *
 * <p>Note: This collector does not attempt to resolve dependencies. It works
 * purely on declared metadata within Gradle configurations.</p>
 *
 * @author Ivan Knanas
 */
public class DefaultDependencyCollector implements DependencyCollector {
    private final Map<String, Set<String>> requestedVersions = new HashMap<>();
    private final Set<String> dependencies = new LinkedHashSet<>();

    /**
     * Collects dependencies from all projects and all configurations.
     *
     * <p>This method iterates over every project in the given Gradle instance,
     * inspects all configurations, and gathers metadata about declared dependencies.
     * For each dependency with a non-null group and name, it:</p>
     *
     * <ul>
     *   <li>Builds a dependency key of the form {@code group:artifact}</li>
     *   <li>Adds the key to the global {@link #dependencies} set</li>
     *   <li>Stores the requested version (if present) in the
     *       {@link #requestedVersions} map under the corresponding key</li>
     * </ul>
     *
     * <p>As a result, the returned set represents all unique
     * {@code group:artifact} pairs across the entire build.</p>
     *
     * @param gradle the current Gradle instance, used to traverse all projects
     *
     * @return a set of collected dependency keys in the format {@code group:artifact}
     */
    public Set<String> collect(Gradle gradle) {
        gradle.allprojects(p -> p.getConfigurations().all(cfg -> {
            for (Dependency d : cfg.getDependencies()) {
                if (d.getGroup() != null && d.getName() != null) {
                    String key = d.getGroup() + ":" + d.getName();
                    dependencies.add(key);
                    requestedVersions.computeIfAbsent(key, k -> new HashSet<>()).add(d.getVersion());
                }
            }
        }));
        return dependencies;
    }

    /**
     * Returns a mapping of all collected dependency keys to their requested versions.
     *
     * <p>The map contains:</p>
     * <ul>
     *   <li>Keys -> {@code group:artifact}</li>
     *   <li>Values -> a set of versions that were explicitly declared for this key</li>
     * </ul>
     *
     * <p>This information is useful for analyzing version conflicts
     * (e.g., when multiple modules in the build request different versions
     * of the same dependency).</p>
     *
     * @return a map of dependency keys to sets of requested versions
     */
    @Override
    public Map<String, Set<String>> getRequestedVersions() {
        return requestedVersions;
    }

}