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

import org.altlinux.xgradle.core.managers.ScopeManager;
import org.altlinux.xgradle.core.managers.TransitiveDependencyManager;
import org.altlinux.xgradle.model.MavenCoordinate;
import org.altlinux.xgradle.services.PomFinder;
import org.gradle.api.logging.Logger;
import java.util.*;

/**
 * Processes transitive dependencies and categorizes them into main and test scopes.
 * <p>
 * This processor:
 * <ul>
 *   <li>Analyzes the dependency tree to identify transitive relationships</li>
 *   <li>Separates dependencies into main and test contexts</li>
 *   <li>Applies scope management rules</li>
 *   <li>Tracks skipped dependencies</li>
 *   <li>Propagates test context flags through the dependency tree</li>
 * </ul>
 *
 * <p>Works in conjunction with {@link TransitiveDependencyManager} to traverse
 * and process the dependency graph.
 *
 * @author Ivan Khanas
 */
public class TransitiveProcessor {
    private final TransitiveDependencyManager transitiveManager;
    private final ScopeManager scopeManager = new ScopeManager();
    private final Set<String> mainDependencies = new HashSet<>();
    private final Set<String> testDependencies = new HashSet<>();
    private final Set<String> testContextDependencies;

    /**
     * Constructs a transitive dependency processor.
     *
     * @param pomFinder Service for locating POM files
     * @param logger Diagnostic logger
     * @param testContextDependencies Pre-identified test-scoped dependencies
     */
    public TransitiveProcessor(PomFinder pomFinder, Logger logger, Set<String> testContextDependencies) {
        this.transitiveManager = new TransitiveDependencyManager(
                pomFinder, logger, this.scopeManager
        );
        this.testContextDependencies = testContextDependencies;
    }

    /**
     * Processes transitive dependencies for all system artifacts.
     * <p>
     * Execution flow:
     * <ol>
     *   <li>Marks test context dependencies in system artifacts</li>
     *   <li>Traverses dependency graph using breadth-first search</li>
     *   <li>Updates dependency scopes based on hierarchy</li>
     *   <li>Categorizes dependencies into main/test contexts</li>
     *   <li>Collects skipped dependencies</li>
     * </ol>
     *
     * @param systemArtifacts Resolved system artifacts (key: "groupId:artifactId")
     */
    public void process(Map<String, MavenCoordinate> systemArtifacts) {
        for (MavenCoordinate coord : systemArtifacts.values()) {
            if (testContextDependencies.contains(coord.getGroupId() + ":" + coord.getArtifactId())) {
                coord.setTestContext(true);
            }
        }

        transitiveManager.processTransitiveDependencies(systemArtifacts);

        for (Map.Entry<String, MavenCoordinate> entry : systemArtifacts.entrySet()) {
            MavenCoordinate coord = entry.getValue();
            if (coord.isTestContext()) {
                testDependencies.add(entry.getKey());
            } else {
                mainDependencies.add(entry.getKey());
            }
        }
    }

    /**
     * Gets main-context dependencies after processing.
     *
     * @return set of dependency identifiers (format: "groupId:artifactId")
     */
    public Set<String> getMainDependencies() {
        return mainDependencies;
    }

    /**
     * Gets test-context dependencies after processing.
     *
     * @return set of dependency identifiers (format: "groupId:artifactId")
     */
    public Set<String> getTestDependencies() {
        return testDependencies;
    }

    /**
     * Gets the scope manager with updated dependency scopes.
     *
     * @return scope manager instance containing scope information
     */
    public ScopeManager getScopeManager() {
        return scopeManager;
    }


    /**
     * Gets dependencies skipped during processing.
     *
     * @return set of skipped dependency identifiers (format: "groupId:artifactId")
     */
    public Set<String> getSkippedDependencies() {
        return transitiveManager.getSkippedDependencies();
    }
}