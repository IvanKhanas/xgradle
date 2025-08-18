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

import org.altlinux.xgradle.model.MavenCoordinate;
import org.gradle.api.invocation.Gradle;

import java.util.Map;
import java.util.Set;

/**
 * Configures resolved artifacts in Gradle project configurations.
 * <p>
 * Implementations of this interface are responsible for adding system-resolved
 * artifacts to appropriate Gradle configurations based on:
 * <ul>
 *   <li>Original dependency declarations</li>
 *   <li>Dependency scope and context</li>
 *   <li>Project configuration metadata</li>
 * </ul>
 *
 * <p>Key responsibilities:
 * <ol>
 *   <li>Filtering out non-deployable artifacts (BOMs, POM-only)</li>
 *   <li>Adding artifacts to explicitly declared configurations</li>
 *   <li>Automatically assigning artifacts to standard configurations
 *       (api, implementation, runtimeOnly, etc.) when no explicit configuration exists</li>
 *   <li>Special handling for test-scoped dependencies</li>
 * </ol>
 *
 * @author Ivan Khanas
 */
public interface ArtifactConfigurator {

    /**
     * Configures artifacts across all projects in the Gradle build.
     *
     * <p>This method processes the resolved artifacts and adds them to appropriate
     * configurations in each project. The configuration logic:
     * <ul>
     *   <li>Uses original configuration names if available (from dependency declarations)</li>
     *   <li>Falls back to scope-based configuration for dependencies without explicit configuration</li>
     *   <li>Handles test dependencies separately (adds to testImplementation)</li>
     *   <li>Skips BOM and POM-type artifacts</li>
     * </ul>
     *
     * @param gradle the Gradle instance representing the build
     * @param artifacts resolved artifacts mapped by dependency key
     * @param dependencyConfigNames mapping of dependency keys to original configuration names
     */
    void configure(
            Gradle gradle,
            Map<String, MavenCoordinate> artifacts,
            Map<String, Set<String>> dependencyConfigNames
    );
}