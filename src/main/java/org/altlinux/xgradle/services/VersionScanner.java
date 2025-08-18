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
package org.altlinux.xgradle.services;

import org.altlinux.xgradle.api.ArtifactVerifier;
import org.altlinux.xgradle.model.MavenCoordinate;

import org.gradle.api.logging.Logger;

import java.util.*;

/**
 * Scans system artifacts to resolve Maven coordinates for project dependencies,
 * including Gradle plugins and dependencies. Manages dependency
 * traversal and verification of artifact existence.
 * <p>
 * This class handles:
 * <ul>
 *   <li>Resolution of regular Maven dependencies</li>
 *   <li>Special handling for Gradle plugin dependencies</li>
 *   <li>Recursive scanning of provided-scope dependencies from POM files</li>
 *   <li>Tracking of unresolved dependencies</li>
 * </ul>
 *
 * @author Ivan Khanas
 */
public class VersionScanner {

    private final PomFinder pomFinder;
    private final ArtifactVerifier artifactVerifier;
    private final Set<String> notFoundDependencies = new HashSet<>();

    /**
     * Constructs a VersionScanner with required service components.
     *
     * @param pomFinder Service for locating POM files in the system
     * @param artifactVerifier Service for verifying artifact existence
     */
    public VersionScanner(PomFinder pomFinder, ArtifactVerifier artifactVerifier) {
        this.pomFinder = pomFinder;
        this.artifactVerifier = artifactVerifier;
    }

    /**
     * Scans system artifacts to resolve Maven coordinates for the given project dependencies.
     * <p>
     * Performs a breadth-first search to:
     * <ol>
     *   <li>Resolve direct project dependencies</li>
     *   <li>Process Gradle plugins using heuristic matching</li>
     *   <li>Recursively resolve dependencies from POM files</li>
     * </ol>
     * Unresolved dependencies are tracked internally and accessible via {@link #getNotFoundDependencies()}.
     *
     * @param projectDependencies  Set of dependency identifiers in "groupId:artifactId" format
     * @param logger Gradle logger for diagnostic messages
     *
     * @return Map linking dependency identifiers to resolved Maven coordinates
     */
    public Map<String, MavenCoordinate> scanSystemArtifacts(Set<String> projectDependencies, Logger logger) {
        notFoundDependencies.clear();
        Map<String, MavenCoordinate> versions = new HashMap<>();
        Set<String> processedDependencies = new HashSet<>();
        Queue<String> dependencyQueue = new LinkedList<>(projectDependencies);

        while (!dependencyQueue.isEmpty()) {
            String dep = dependencyQueue.poll();
            if (processedDependencies.contains(dep)) continue;
            processedDependencies.add(dep);
            if (versions.containsKey(dep)) continue;

            if (dep.endsWith(".gradle.plugin")) {
                resolveGradlePlugin(dep, versions, logger);
            } else {
                resolveRegularDependency(dep, versions, logger);
            }

            MavenCoordinate coord = versions.get(dep);
            if (coord != null && coord.getPomPath() != null) {
                scanProvidedDependencies(coord, dependencyQueue, versions, logger);
            }
        }
        return versions;
    }

    /**
     * Scans and enqueues provided-scope and runtime-scope dependencies from a parent artifact's POM.
     *
     * <p>This method:
     * <ul>
     *   <li>Parses dependencies from the parent artifact's POM file</li>
     *   <li>Filters dependencies with "provided" or "runtime" scope</li>
     *   <li>Enqueues new dependencies for processing if:
     *     <ul>
     *       <li>They haven't been resolved yet</li>
     *       <li>They're not already in the processing queue</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <p>This enables recursive resolution of scope-specific dependencies that might be required
     * for compilation or runtime but aren't directly declared in the project.
     *
     * @param parentCoord     Parent artifact coordinates containing the POM to scan
     * @param dependencyQueue Queue of dependencies pending resolution
     * @param versions        Map of already resolved dependencies
     * @param logger          Gradle logger for diagnostic messages
     */
    private void scanProvidedDependencies(MavenCoordinate parentCoord,
                                          Queue<String> dependencyQueue,
                                          Map<String, MavenCoordinate> versions,
                                          Logger logger) {
        List<MavenCoordinate> dependencies = pomFinder.getPomParser()
                .parseDependencies(parentCoord.getPomPath(), logger);

        for (MavenCoordinate dep : dependencies) {
            if ("provided".equals(dep.getScope()) || "runtime".equals(dep.getScope())) {
                String depKey = dep.getGroupId() + ":" + dep.getArtifactId();
                if (!versions.containsKey(depKey) && !dependencyQueue.contains(depKey)) {
                    dependencyQueue.add(depKey);
                }
            }
        }
    }

    /**
     * Resolves Gradle plugin dependencies using heuristic matching.
     * <p>
     * This method:
     * <ul>
     *   <li>Splits plugin ID to extract base name</li>
     *   <li>Tries multiple artifact naming patterns (pluginId, pluginId-plugin, etc.)</li>
     *   <li>Attempts to locate matching POM file and verify artifact existence</li>
     *   <li>Adds successfully resolved plugins to the versions map</li>
     * </ul>
     *
     * @param pluginDep Plugin dependency identifier in "id:gradle.plugin" format
     * @param versions Map to store resolved dependencies
     * @param logger Gradle logger for diagnostic messages
     */
    private void resolveGradlePlugin(String pluginDep, Map<String, MavenCoordinate> versions, Logger logger) {
        String[] parts = pluginDep.split(":");
        if (parts.length != 2) return;

        MavenCoordinate pom = findPluginArtifact(parts[0], logger);
        if (pom != null && artifactVerifier.verifyArtifactExists(pom, logger)) {
            versions.put(pluginDep, pom);
        }
    }

    /**
     * Resolves regular dependencies (non-plugin).
     * <p>
     * This method:
     * <ul>
     *   <li>Parses groupId and artifactId from dependency string</li>
     *   <li>Skips dependencies with placeholder values</li>
     *   <li>Attempts to locate matching POM file</li>
     *   <li>Verifies artifact existence in the filesystem</li>
     *   <li>Tracks unresolved dependencies in notFoundDependencies set</li>
     * </ul>
     *
     * @param dep Dependency identifier in "groupId:artifactId" format
     * @param versions Map to store resolved dependencies
     * @param logger Gradle logger for diagnostic messages
     */
    private void resolveRegularDependency(String dep, Map<String, MavenCoordinate> versions, Logger logger) {
        String[] parts = dep.split(":");
        if (parts.length < 2) return;

        String groupId = parts[0];
        String artifactId = parts[1];

        if (hasPlaceholder(groupId) || hasPlaceholder(artifactId)) return;

        MavenCoordinate pom = pomFinder.findPomForArtifact(groupId, artifactId, logger);
        if (pom == null) {
            notFoundDependencies.add(dep);
            return;
        }
        if (!artifactVerifier.verifyArtifactExists(pom, logger)) {
            notFoundDependencies.add(dep);
            return;
        }
        versions.put(dep, pom);
    }

    /**
     * Checks if a string contains a Maven placeholder pattern.
     * <p>
     * Placeholder pattern is defined as ${...} (e.g., ${version}).
     *
     * @param value String to check for placeholders
     *
     * @return true if the string contains a placeholder, false otherwise
     */
    private boolean hasPlaceholder(String value) {
        return value != null && value.contains("${");
    }

    /**
     * Finds the main artifact for a Gradle plugin by trying multiple naming conventions.
     * <p>
     * This method:
     * <ul>
     *   <li>Generates multiple artifact naming patterns based on pluginId</li>
     *   <li>Tries standard plugin naming conventions (pluginId, pluginId-plugin, etc.)</li>
     *   <li>Handles domain-style plugin IDs (converts to hyphenated format)</li>
     *   <li>Falls back to searching for artifacts containing "gradle" or "plugin" in name</li>
     * </ul>
     *
     * @param pluginId Gradle plugin ID (e.g., "org.example.myplugin")
     * @param logger Gradle logger for diagnostic messages
     *
     * @return MavenCoordinate of the resolved plugin artifact, or null if not found
     */
    public MavenCoordinate findPluginArtifact(String pluginId, Logger logger) {
        String baseName = pluginId.contains(".") ?
                pluginId.substring(pluginId.lastIndexOf('.') + 1) : pluginId;

        String[] artifactIds = {
                baseName, baseName + "-plugin", "gradle-" + baseName,
                "gradle-" + baseName + "-plugin", baseName + "-gradle-plugin",
                "gradle-plugin-" + baseName, baseName + "-gradle"
        };

        for (String artifactId : artifactIds) {
            MavenCoordinate coord = pomFinder.findPomForArtifact(pluginId, artifactId, logger);
            if (coord != null && artifactVerifier.verifyArtifactExists(coord, logger)) {
                return coord;
            }
        }

        if (pluginId.contains(".")) {
            String withoutDomain = pluginId.substring(pluginId.indexOf('.') + 1).replace('.', '-');
            String[] extendedArtifactIds = {
                    withoutDomain, withoutDomain + "-plugin", "gradle-" + withoutDomain,
                    "gradle-" + withoutDomain + "-plugin", withoutDomain + "-gradle-plugin"
            };

            for (String artifactId : extendedArtifactIds) {
                MavenCoordinate coord = pomFinder.findPomForArtifact(pluginId, artifactId, logger);
                if (coord != null && artifactVerifier.verifyArtifactExists(coord, logger)) {
                    return coord;
                }
            }
        }

        return findMainArtifactForGroup(pluginId, logger);
    }

    /**
     * Finds the main artifact for a group when direct plugin matching fails.
     * <p>
     * This method:
     * <ul>
     *   <li>Searches for all POM files belonging to the specified groupId</li>
     *   <li>Filters artifacts containing "gradle" or "plugin" in their artifactId</li>
     *   <li>Returns the first matching artifact (if any)</li>
     * </ul>
     *
     * @param groupId Maven group ID to search for
     * @param logger Gradle logger for diagnostic messages
     *
     * @return MavenCoordinate of the main Gradle plugin artifact, or null if not found
     */
    private MavenCoordinate findMainArtifactForGroup(String groupId, Logger logger) {
        ArrayList<MavenCoordinate> candidates = pomFinder.findAllPomsForGroup(groupId, logger);
        return candidates.stream()
                .filter(coord -> coord.getArtifactId().contains("gradle") ||
                        coord.getArtifactId().contains("plugin"))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves dependencies that could not be resolved during the last scan.
     * <p>
     * The returned set contains dependency identifiers in "groupId:artifactId" format
     * that failed either POM location or artifact verification.
     *
     * @return Set of unresolved dependency identifiers (empty if all resolved)
     */
    public Set<String> getNotFoundDependencies() {
        return notFoundDependencies;
    }
}