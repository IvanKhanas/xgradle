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
package org.altlinux.xgradle.impl.processors;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.altlinux.xgradle.interfaces.managers.TransitiveDependencyManager;
import org.altlinux.xgradle.interfaces.processors.TransitiveProcessor;
import org.altlinux.xgradle.interfaces.processors.TransitiveResult;
import org.altlinux.xgradle.impl.enums.MavenScope;
import org.altlinux.xgradle.impl.model.MavenCoordinate;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Applies test-context markers to root artifacts and delegates transitive traversal to TransitiveDependencyManager.
 * Implements {@link TransitiveProcessor}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class ScopeAwareTransitiveProcessor implements TransitiveProcessor {

    private final TransitiveDependencyManager transitiveManager;

    @Inject
    ScopeAwareTransitiveProcessor(TransitiveDependencyManager transitiveManager) {
        this.transitiveManager = transitiveManager;
    }

    @Override
    public TransitiveResult process(
            Map<String, MavenCoordinate> systemArtifacts,
            Set<String> testContextDependencies,
            Map<String, MavenScope> dependencyScopes,
            Map<String, Set<String>> dependencyConfigNames
    ) {
        if (systemArtifacts == null || systemArtifacts.isEmpty()) {
            return TransitiveResult.empty();
        }

        Set<String> testContext = (testContextDependencies == null)
                ? Collections.emptySet()
                : testContextDependencies;

        systemArtifacts.replaceAll((artifactKey, coordinate) -> {
            if (coordinate == null) {
                return null;
            }
            if (testContext.contains(artifactKey) && !coordinate.isTestContext()) {
                return coordinate.toBuilder()
                        .testContext(true)
                        .build();
            }
            return coordinate;
        });

        Set<String> skippedDependencies = transitiveManager.configure(
                systemArtifacts,
                dependencyScopes,
                dependencyConfigNames
        );

        Map<Boolean, Set<String>> partitionedDependencies = systemArtifacts.entrySet().stream()
                .collect(Collectors.partitioningBy(
                        entry -> entry.getValue() != null && entry.getValue().isTestContext(),
                        Collectors.mapping(Map.Entry::getKey, Collectors.toSet())
                ));

        Set<String> mainDependencies = new HashSet<>(
                partitionedDependencies.getOrDefault(false, Collections.emptySet())
        );
        Set<String> testDependencies = new HashSet<>(
                partitionedDependencies.getOrDefault(true, Collections.emptySet())
        );

        return new TransitiveResult(mainDependencies, testDependencies, skippedDependencies);
    }
}
