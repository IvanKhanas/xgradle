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
package org.altlinux.xgradle.core.processors;

import org.altlinux.xgradle.model.MavenCoordinate;
import org.altlinux.xgradle.services.PomFinder;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import java.util.*;

/**
 * Processes BOM (Bill of Materials) dependencies in Gradle projects.
 * <p>
 * This processor handles:
 * <ul>
 *   <li>Identification of BOM dependencies in project configurations</li>
 *   <li>Recursive processing of transitive BOM dependencies</li>
 *   <li>Collection of dependency versions managed through BOMs</li>
 *   <li>Marking of test-scoped dependencies from test BOMs</li>
 *   <li>Post-processing removal of BOMs from dependency configurations</li>
 * </ul>
 *
 * <p>The core processing occurs in {@link #process(Set, PomFinder, Logger)} which expands
 * the project's dependency set by incorporating BOM-managed dependencies.
 *
 * @author Ivan Khanas
 */
public class BomProcessor {
    private final Map<String, List<String>> bomManagedDeps = new LinkedHashMap<>();
    private final Set<String> processedBoms = new HashSet<>();
    private final Map<String, String> managedVersions = new HashMap<>();

    /**
     * Processes the given set of project dependencies to identify and expand BOM-managed dependencies.
     *
     * <p>This method:
     * <ul>
     *   <li>Identifies BOM dependencies among the given project dependencies</li>
     *   <li>Recursively processes transitive BOMs</li>
     *   <li>Collects managed dependency versions into an internal registry</li>
     *   <li>Maintains a mapping of BOMs to their managed dependencies</li>
     * </ul>
     *
     * @param projectDependencies the initial set of project dependencies (as {@code groupId:artifactId[:version]} strings)
     * @param pomFinder           a service used to resolve and parse POM files
     * @param logger              a logger for reporting processing steps
     *
     * @return the original set of project dependencies (unchanged),
     *         with side effects recorded in {@link #getBomManagedDeps()} and {@link #getManagedVersions()}
     */
    public Set<String> process(Set<String> projectDependencies, PomFinder pomFinder, Logger logger) {
        managedVersions.clear();
        Queue<MavenCoordinate> bomQueue = new LinkedList<>();

        for (String dep : projectDependencies) {
            String[] parts = dep.split(":");
            if (parts.length < 2) continue;
            MavenCoordinate coord = pomFinder.findPomForArtifact(parts[0], parts[1], logger);
            if (coord != null && coord.isBom()) {
                bomQueue.add(coord);
                String bomKey = coord.getGroupId() + ":" + coord.getArtifactId();
                processedBoms.add(bomKey);
            }
        }

        while (!bomQueue.isEmpty()) {
            MavenCoordinate bom = bomQueue.poll();
            String bomKey = bom.getGroupId() + ":" + bom.getArtifactId() + ":" + bom.getVersion();
            List<String> managedDeps = new ArrayList<>();
            List<MavenCoordinate> dependencies = pomFinder.getPomParser()
                    .parseDependencyManagement(bom.getPomPath(), logger);

            for (MavenCoordinate dep : dependencies) {
                String depKey = dep.getGroupId() + ":" + dep.getArtifactId();
                managedVersions.put(depKey, dep.getVersion());
                managedDeps.add(depKey + ":" + dep.getVersion());

                if (dep.isBom() && !processedBoms.contains(depKey)) {
                    bomQueue.add(dep);
                    processedBoms.add(depKey);
                }
            }
            bomManagedDeps.put(bomKey, managedDeps);
        }
        return projectDependencies;
    }

    /**
     * Removes all BOM dependencies from Gradle project configurations.
     *
     * <p>This method iterates through all configurations in all projects and removes
     * dependencies that were previously identified as BOMs during {@link #process(Set, PomFinder, Logger)}.
     *
     * @param gradle the Gradle invocation object, providing access to all projects and configurations
     */
    public void removeBomsFromConfigurations(Gradle gradle) {
        gradle.allprojects(p -> p.getConfigurations().all(cfg -> {
            List<Dependency> toRemove = new ArrayList<>();
            for (Dependency d : cfg.getDependencies()) {
                String key = d.getGroup() + ":" + d.getName();
                if (processedBoms.contains(key)) toRemove.add(d);
            }
            toRemove.forEach(dep -> cfg.getDependencies().remove(dep));
        }));
    }

    /**
     * Returns a mapping of BOM coordinates to the list of dependencies they manage.
     *
     * <p>Each key represents a BOM in the format {@code groupId:artifactId:version},
     * and each value is a list of managed dependency strings in the format {@code groupId:artifactId:version}.
     *
     * @return a map of BOMs to their managed dependencies
     */
    public Map<String, List<String>> getBomManagedDeps() {
        return bomManagedDeps;
    }

    /**
     * Returns the resolved versions of dependencies managed through BOMs.
     *
     * <p>The map keys are {@code groupId:artifactId}, and the values are the
     * resolved versions provided by BOM dependency management.
     *
     * @return a map of managed dependency versions
     */
    public Map<String, String> getManagedVersions() {
        return managedVersions;
    }
}