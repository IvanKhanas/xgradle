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
package org.altlinux.gradlePlugin.core.resolvers;

import org.altlinux.gradlePlugin.model.MavenCoordinate;
import org.altlinux.gradlePlugin.services.VersionScanner;

import org.gradle.api.logging.Logger;

import java.util.Map;
import java.util.Set;

/**
 * Resolves and filters system artifacts for dependency management.
 *
 * <p>Handles two core operations:
 * <ol>
 *   <li>Locating artifacts in system repositories using {@link VersionScanner}</li>
 *   <li>Filtering out unnecessary artifacts (test-scoped and BOMs)</li>
 * </ol>
 *
 * <p>Typical workflow:
 * <pre>
 * 1. Resolve dependencies
 * resolver.resolve(dependencies, logger);
 *
 * 2. Apply filters
 * resolver.filter();
 *
 * 3. Use results
 * Map&lt;String, MavenCoordinate&gt; artifacts = resolver.getSystemArtifacts();
 * </pre>
 *
 * @author Ivan Khanas
 */
public class ArtifactResolver {
    private final VersionScanner versionScanner;
    private Map<String, MavenCoordinate> systemArtifacts;

    /**
     * Creates resolver with specific version scanner.
     * @param versionScanner Scanner implementation to locate artifacts
     */
    public ArtifactResolver(VersionScanner versionScanner) {
        this.versionScanner = versionScanner;
    }

    /**
     * Locates system artifacts for requested dependencies.
     *
     * @param dependencies Dependency keys to resolve (format: "group:artifact")
     * @param logger For progress reporting and diagnostics
     */
    public void resolve(Set<String> dependencies, Logger logger) {
        systemArtifacts = versionScanner.scanSystemArtifacts(dependencies, logger);
    }

    /**
     * Removes artifacts that shouldn't be included in builds:
     * <ul>
     *   <li>Test-scoped dependencies (scope = "test")</li>
     *   <li>BOM artifacts (packaging = "pom")</li>
     * </ul>
     */
    public void filter() {
        systemArtifacts.entrySet().removeIf(e ->
                "test".equals(e.getValue().getScope()) || e.getValue().isBom()
        );
    }

    /**
     * @return Resolved artifacts after filtering (key format: "group:artifact")
     */
    public Map<String, MavenCoordinate> getSystemArtifacts() {
        return systemArtifacts;
    }

    /**
     * @return Dependency keys that couldn't be resolved (format: "group:artifact")
     */
    public Set<String> getNotFoundDependencies() {
        return versionScanner.getNotFoundDependencies();
    }
}