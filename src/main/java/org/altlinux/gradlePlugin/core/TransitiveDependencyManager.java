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

    public Set<String> processTransitiveDependencies(
            Set<String> projectDependencies,
            Map<String, MavenCoordinate> systemArtifacts,
            Set<String> bomManagedDeps) {

        logger.lifecycle(">>> Processing transitive dependencies");
        Set<String> allDependencies = new HashSet<>(projectDependencies);
        Queue<MavenCoordinate> queue = new LinkedList<>();

        for (String dep : projectDependencies) {
            MavenCoordinate coord = systemArtifacts.get(dep);
            if (coord != null && coord.pomPath != null && !processedArtifacts.contains(dep)) {
                logger.info("Adding to queue: {}", dep);
                queue.add(coord);
                processedArtifacts.add(dep);
            }
        }

        while (!queue.isEmpty()) {
            MavenCoordinate current = queue.poll();
            logger.info("Processing artifact: {}:{}:{}",
                    current.groupId, current.artifactId, current.version);

            List<MavenCoordinate> dependencies = pomFinder.getPomParser()
                    .parseDependencies(current.pomPath, logger);

            for (MavenCoordinate dep : dependencies) {
                String depKey = dep.groupId + ":" + dep.artifactId;
                logger.info("Found dependency: {} (scope: {}, packaging: {})",
                        depKey, dep.scope, dep.packaging);

                boolean isBomManaged = bomManagedDeps.contains(depKey);

                if ("provided".equals(dep.scope)) {
                    logger.info("Handling PROVIDED dependency: {}", depKey);
                    transitiveDependencies.add(dep);
                    if (systemArtifacts.containsKey(depKey)) {
                        logger.info("Provided dependency {} available in system", depKey);
                    } else {
                        skippedDependencies.add("Provided: " + depKey);
                    }
                    continue;
                }

                if ("test".equals(dep.scope)) {
                    logger.info("Skipping TEST dependency: {}", depKey);
                    skippedDependencies.add("Test: " + depKey);
                    continue;
                }

                if (containsPlaceholder(dep)) {
                    logger.info("Skipping dependency with PLACEHOLDER: {}", depKey);
                    skippedDependencies.add("Placeholder: " + depKey);
                    continue;
                }

                if ("pom".equals(dep.packaging)) {
                    logger.info("Skipping BOM dependency: {}", depKey);
                    skippedDependencies.add("BOM: " + depKey);
                    continue;
                }

                if (hasPlaceholder(dep.version)) {
                    Map<String, String> properties = pomFinder.getPomParser()
                            .parseProperties(current.pomPath, logger);

                    String propertyName = extractPropertyName(dep.version);
                    if (propertyName != null && properties.containsKey(propertyName)) {
                        dep.version = properties.get(propertyName);
                    }
                }

                MavenCoordinate resolvedDep = pomFinder.findPomForArtifact(
                        dep.groupId, dep.artifactId, logger);

                if (resolvedDep == null) {
                    logger.info("Dependency NOT FOUND: {}", depKey);
                    skippedDependencies.add("Not found: " + depKey);
                    continue;
                }

                if (!artifactVerifier.verifyArtifactExists(resolvedDep, logger)) {
                    logger.info("Artifact MISSING: {}", depKey);
                    skippedDependencies.add("Artifact missing: " + depKey);
                    continue;
                }

                if ("pom".equals(resolvedDep.packaging)) {
                    logger.info("Skipping BOM transitive: {}", depKey);
                    skippedDependencies.add("BOM transitive: " + depKey);
                    continue;
                }

                resolvedDep.scope = dep.scope;
                resolvedDep.packaging = dep.packaging;

                transitiveDependencies.add(resolvedDep);
                logger.info("Added transitive dependency: {} (scope: {})", depKey, dep.scope);

                if (!isBomManaged && !projectDependencies.contains(depKey)) {
                    trueTransitiveDeps.add(depKey);
                    logger.info("Added to true transitive dependencies: {}", depKey);
                }

                if (!processedArtifacts.contains(depKey)) {
                    logger.info("Queueing for further processing: {}", depKey);
                    processedArtifacts.add(depKey);
                    allDependencies.add(depKey);
                    queue.add(resolvedDep);
                }
            }
        }

        logger.lifecycle("<<< Finished processing transitive dependencies. Total: {}", trueTransitiveDeps.size());
        logger.info("Transitive dependencies list:");
        for (MavenCoordinate coord : transitiveDependencies) {
            logger.info(" - {}:{}:{} (scope: {})",
                    coord.groupId, coord.artifactId, coord.version, coord.scope);
        }
        return trueTransitiveDeps;
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
        return hasPlaceholder(coord.groupId) ||
                hasPlaceholder(coord.artifactId) ||
                hasPlaceholder(coord.version);
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