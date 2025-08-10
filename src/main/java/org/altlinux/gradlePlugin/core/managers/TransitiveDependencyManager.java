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
package org.altlinux.gradlePlugin.core.managers;

import org.altlinux.gradlePlugin.api.ArtifactVerifier;
import org.altlinux.gradlePlugin.model.MavenCoordinate;
import org.altlinux.gradlePlugin.services.PomFinder;

import org.gradle.api.logging.Logger;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Manages transitive dependency resolution in Gradle projects.
 * Performs breadth-first traversal of dependency trees while handling
 * scope filtering and version placeholder resolution.
 *
 * @author Ivan Khanas
 */
public class TransitiveDependencyManager {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(.*?)}");

    private final PomFinder pomFinder;
    private final ArtifactVerifier artifactVerifier;
    private final Logger logger;

    private final Set<String> processedArtifacts = new HashSet<>();
    private final Set<MavenCoordinate> transitiveDependencies = new HashSet<>();
    private final Set<String> skippedDependencies = new HashSet<>();
    private final Set<String> trueTransitiveDeps = new HashSet<>();

    /**
     * Creates a new BOM manager with required services.
     *
     * @param pomFinder service for locating POM files
     * @param logger Gradle logger instance
     */
    public TransitiveDependencyManager(
            PomFinder pomFinder,
            ArtifactVerifier artifactVerifier,
            Logger logger) {
        this.pomFinder = pomFinder;
        this.artifactVerifier = artifactVerifier;
        this.logger = logger;
    }

    public void processTransitiveDependencies(Map<String, MavenCoordinate> systemArtifacts) {
        logger.lifecycle(">>> Processing transitive dependencies");
        Queue<MavenCoordinate> queue = new LinkedList<>(systemArtifacts.values());

        while (!queue.isEmpty()) {
            MavenCoordinate current = queue.poll();
            if (current.getPomPath() == null) continue;

            List<MavenCoordinate> dependencies = pomFinder.getPomParser()
                    .parseDependencies(current.getPomPath(), logger);

            for (MavenCoordinate dep : dependencies) {
                String depKey = dep.getGroupId() + ":" + dep.getArtifactId();

                if ("test".equals(dep.getScope())) continue;

                MavenCoordinate resolvedDep = systemArtifacts.get(depKey);
                if (resolvedDep == null) {
                    resolvedDep = pomFinder.findPomForArtifact(
                            dep.getGroupId(), dep.getArtifactId(), logger
                    );
                    if (resolvedDep == null) {
                        logger.warn("Skipping not found dependency: {}", depKey);
                        skippedDependencies.add(depKey);
                        continue;
                    }
                    systemArtifacts.put(depKey, resolvedDep);
                }

                resolvedDep.setTestContext(current.isTestContext());

                if (!processedArtifacts.contains(depKey)) {
                    processedArtifacts.add(depKey);
                    queue.add(resolvedDep);
                }
            }
        }
    }

    /**
     * Extracts property name from Maven placeholder.
     *
     * @param value string containing placeholder pattern
     * @return extracted property name or null
     */
    private String extractPropertyName(String value) {
        if (value == null) return null;
        java.util.regex.Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Checks if Maven coordinate contains placeholders.
     *
     * @param coord Maven coordinate to check
     * @return true if contains placeholders
     */
    private boolean containsPlaceholder(MavenCoordinate coord) {
        return hasPlaceholder(coord.getGroupId()) ||
                hasPlaceholder(coord.getArtifactId()) ||
                hasPlaceholder(coord.getVersion());
    }

    /**
     * Checks if string contains placeholder.
     *
     * @return true if contains and false otherwise
     */
    private boolean hasPlaceholder(String value) {
        return value != null && PLACEHOLDER_PATTERN.matcher(value).find();
    }

    /**
     * Returns all resolved transitive dependencies.
     *
     * @return set of Maven coordinates
     */
    public Set<MavenCoordinate> getTransitiveDependencies() {
        return transitiveDependencies;
    }

    /**
     * Returns skipped dependencies with reasons.
     *
     * @return set of skipped dependency descriptions
     */
    public Set<String> getSkippedDependencies() {
        return skippedDependencies;
    }
}