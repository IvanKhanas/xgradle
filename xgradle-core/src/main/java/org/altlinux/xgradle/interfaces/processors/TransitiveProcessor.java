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
package org.altlinux.xgradle.interfaces.processors;

import org.altlinux.xgradle.impl.enums.MavenScope;
import org.altlinux.xgradle.impl.model.MavenCoordinate;

import java.util.Map;
import java.util.Set;

/**
 * Processor that categorizes dependencies into main/test scopes
 * based on transitive dependency graph.
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface TransitiveProcessor {

    /**
     * Processes transitive dependencies and categorizes them into main/test contexts.
     *
     * @param systemArtifacts map of system artifacts (key: "groupId:artifactId")
     * @param testContextDependencies dependency keys that should be marked as test context
     * @param dependencyScopes mutable scope map to update during traversal
     * @return result with main/test/skipped dependency keys
     */
    TransitiveResult process(
            Map<String, MavenCoordinate> systemArtifacts,
            Set<String> testContextDependencies,
            Map<String, MavenScope> dependencyScopes,
            Map<String, Set<String>> dependencyConfigNames
    );
}
