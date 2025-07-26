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
import org.altlinux.gradlePlugin.services.PomFinder;
import org.gradle.api.logging.Logger;
import java.util.*;

/**
 * Manages BOM (Bill of Materials) dependencies by processing project dependencies,
 * resolving BOMs recursively, and collecting their managed dependencies.
 *
 * <p>Uses {@link PomFinder} to locate and parse BOM POM files. Tracks processed BOMs
 * to avoid duplicates and aggregates managed dependencies.
 *
 * <p>Typical usage: provide project dependencies to {@link #processBomDependencies(Set)},
 * then access managed dependencies and processed BOMs via getters.
 *
 * @author Ivan Khanas
 */
public class BomDependencyManager
{
    private final PomFinder pomFinder;
    private final Logger logger;

    private final Set<String> processedBoms = new HashSet<>();
    private final Set<String> managedDependencies = new HashSet<>();

    private final Map<String, List<String>> bomManagedDeps = new LinkedHashMap<>();

    /**
     * Creates a BomDependencyManager with the specified PomFinder and Logger.
     *
     * @param pomFinder used to locate and parse POM files
     * @param logger used for logging messages and errors
     */
    public BomDependencyManager(PomFinder pomFinder, Logger logger) {
        this.pomFinder = pomFinder;
        this.logger = logger;
    }

    /**
     * Processes the given project dependencies to resolve BOMs recursively,
     * collects all managed dependencies, and returns the full set of dependencies.
     *
     * @param projectDependencies initial set of project dependencies (groupId:artifactId)
     *
     * @return a set of all dependencies including those managed by BOMs
     */
    public Set<String> processBomDependencies(Set<String> projectDependencies) {
        managedDependencies.clear();
        Set<String> allDependencies = new HashSet<>(projectDependencies);
        Queue<MavenCoordinate> bomQueue = new LinkedList<>();

        for (String dep : projectDependencies) {
            String[] parts = dep.split(":");
            if (parts.length < 2) continue;

            MavenCoordinate coord = pomFinder.findPomForArtifact(parts[0], parts[1], logger);
            if (coord != null && coord.isBom()) {
                bomQueue.add(coord);
                processedBoms.add(coord.groupId + ":" + coord.artifactId);
            }
        }

        while (!bomQueue.isEmpty()) {
            MavenCoordinate bom = bomQueue.poll();
            String bomKey = bom.groupId + ":" + bom.artifactId + ":" + bom.version;
            List<String> managedDeps = new ArrayList<>();

            List<MavenCoordinate> dependencies = pomFinder.getPomParser().parseDependencyManagement(bom.pomPath, logger);
            for (MavenCoordinate dep : dependencies) {
                String depKey = dep.groupId + ":" + dep.artifactId;
                managedDeps.add(depKey + ":" + dep.version);
                managedDependencies.add(depKey);

                if (dep.isBom() && !processedBoms.contains(depKey)) {
                    bomQueue.add(dep);
                    processedBoms.add(depKey);
                }

                if (!allDependencies.contains(depKey)) {
                    allDependencies.add(depKey);
                }
            }
            bomManagedDeps.put(bomKey, managedDeps);
        }

        return allDependencies;
    }

    /**
     * Returns a map of BOM coordinates to their managed dependencies.
     *
     * @return map where keys are BOM identifiers and values are lists of managed dependencies
     */
    public Map<String, List<String>> getBomManagedDeps() {
        return bomManagedDeps;
    }

    /**
     * Returns the set of BOMs that have been processed.
     *
     * @return set of processed BOM identifiers (groupId:artifactId)
     */
    public Set<String> getProcessedBoms() {
        return processedBoms;
    }

    /**
     * Returns the set of dependencies managed by the processed BOMs.
     *
     * @return set of managed dependency identifiers (groupId:artifactId)
     */
    public Set<String> getManagedDependencies() {
        return managedDependencies;
    }
}