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
 * Resolves system artifacts and filters invalid dependencies.
 */
public class ArtifactResolver {
    private final VersionScanner versionScanner;
    private Map<String, MavenCoordinate> systemArtifacts;

    public ArtifactResolver(VersionScanner versionScanner) {
        this.versionScanner = versionScanner;
    }

    /**
     * Resolves system artifacts for given dependencies.
     *
     * @param dependencies dependencies to resolve
     * @param logger Gradle logger
     */
    public void resolve(Set<String> dependencies, Logger logger) {
        systemArtifacts = versionScanner.scanSystemArtifacts(dependencies, logger);
    }

    /**
     * Filters out test-scoped and BOM artifacts.
     */
    public void filter() {
        systemArtifacts.entrySet().removeIf(e ->
                "test".equals(e.getValue().getScope()) || e.getValue().isBom()
        );
    }

    public Map<String, MavenCoordinate> getSystemArtifacts() {
        return systemArtifacts;
    }

    public Set<String> getNotFoundDependencies() {
        return versionScanner.getNotFoundDependencies();
    }
}