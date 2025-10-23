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
package org.altlinux.xgradle.api;

import org.gradle.api.invocation.Gradle;

import java.util.Map;
import java.util.Set;

/**
 * Collects and aggregates dependency information from all projects in a Gradle build.
 * <p>
 * Implementations of this interface traverse the entire project hierarchy, inspecting
 * all configurations to gather metadata about declared dependencies. The collector
 * provides two key pieces of information:
 * <ul>
 *   <li>A deduplicated set of dependency identifiers (format: "groupId:artifactId")</li>
 *   <li>A mapping of each dependency to all versions requested for it across the build</li>
 * </ul>
 *
 * <p>Typical usage:
 * <ol>
 *   <li>Call {@link #collect(Gradle)} to gather dependency identifiers</li>
 *   <li>Use {@link #getRequestedVersions()} to analyze version requirements</li>
 * </ol>
 *
 * <p>The collected data serves as input for dependency resolution and version
 * alignment processes.
 *
 * @author Ivan Khanas
 */
public interface DependencyCollector {

    /**
     * Collects dependency identifiers from all projects and configurations.
     *
     * <p>This method traverses every project in the Gradle instance and inspects
     * all configurations, gathering identifiers for explicitly declared dependencies.
     * For each dependency with a non-null group and name, it:
     * <ul>
     *   <li>Builds a dependency key of the form {@code groupId:artifactId}</li>
     *   <li>Adds the key to the returned set</li>
     *   <li>Records the requested version in the internal version tracking map</li>
     * </ul>
     *
     * <p>The returned set represents all unique dependency identifiers across
     * the entire build.
     *
     * @param gradle the Gradle instance to traverse
     * @return a set of collected dependency keys in "groupId:artifactId" format
     */
    Set<String> collect(Gradle gradle);

    /**
     * Returns a mapping of dependency keys to their requested versions.
     *
     * <p>The map contains:
     * <ul>
     *   <li>Keys -> dependency identifiers (format: "groupId:artifactId")</li>
     *   <li>Values -> sets of versions explicitly requested for each dependency</li>
     * </ul>
     *
     * <p>This information is essential for:
     * <ul>
     *   <li>Identifying version conflicts across modules</li>
     *   <li>Determining version alignment requirements</li>
     *   <li>Logging original dependency declarations</li>
     * </ul>
     *
     * @return a map of dependency keys to sets of requested versions
     */
    Map<String, Set<String>> getRequestedVersions();
}
