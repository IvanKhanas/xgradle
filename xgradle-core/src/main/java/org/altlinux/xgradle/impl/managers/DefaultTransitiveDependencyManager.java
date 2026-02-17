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
package org.altlinux.xgradle.impl.managers;

import com.google.inject.Inject;

import com.google.inject.Singleton;
import org.altlinux.xgradle.interfaces.managers.ScopeManager;
import org.altlinux.xgradle.interfaces.managers.TransitiveDependencyManager;
import org.altlinux.xgradle.interfaces.maven.PomFinder;

import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.altlinux.xgradle.impl.enums.MavenScope;
import org.altlinux.xgradle.impl.model.MavenCoordinate;

import org.gradle.api.logging.Logger;
import java.util.*;

import static org.altlinux.xgradle.impl.utils.logging.LogPainter.green;

/**
 * Manages transitive dependency resolution in Gradle projects.
 * Implements {@link TransitiveDependencyManager}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultTransitiveDependencyManager implements TransitiveDependencyManager {
    private final PomFinder pomFinder;
    private final PomParser pomParser;
    private final Logger logger;

    private final ScopeManager mavenScopeManager;

    @Inject
    DefaultTransitiveDependencyManager(
            PomFinder pomFinder,
            PomParser pomParser,
            ScopeManager mavenScopeManager,
            Logger logger
            ) {
        this.pomFinder = pomFinder;
        this.pomParser = pomParser;
        this.mavenScopeManager = mavenScopeManager;
        this.logger = logger;
    }

    @Override
    public Set<String> configure(
            Map<String, MavenCoordinate> systemArtifacts,
            Map<String, MavenScope> dependencyScopes,
            Map<String, Set<String>> dependencyConfigNames
    ) {
        Set<String> processedArtifacts = new HashSet<>();
        Set<String> skippedDependencies = new HashSet<>();

        logger.lifecycle(green(">>> Processing transitive dependencies"));
        Queue<MavenCoordinate> queue = new LinkedList<>(systemArtifacts.values());

        while (!queue.isEmpty()) {
            MavenCoordinate current = queue.poll();
            if (current.getPomPath() == null) continue;
            if (current.getGroupId() == null || current.getArtifactId() == null) {
                continue;
            }

            String currentKey = current.getGroupId() + ":" + current.getArtifactId();
            Set<String> parentConfigs = dependencyConfigNames != null
                    ? dependencyConfigNames.get(currentKey)
                    : null;

            List<MavenCoordinate> dependencies = pomParser
                    .parseDependencies(current.getPomPath());

            for (MavenCoordinate dep : dependencies) {
                String depKey = dep.getGroupId() + ":" + dep.getArtifactId();
                mavenScopeManager.updateScope(dependencyScopes, depKey, dep.getScope());
                if (MavenScope.TEST.equals(dep.getScope())) continue;

                if (parentConfigs != null && !parentConfigs.isEmpty()) {
                    Set<String> standardConfigs = filterStandardConfigurations(parentConfigs);
                    if (standardConfigs.isEmpty()) {
                        skippedDependencies.add(depKey);
                        continue;
                    }

                    dependencyConfigNames
                            .computeIfAbsent(depKey, k -> new HashSet<>())
                            .addAll(standardConfigs);
                }

                MavenCoordinate resolvedDep = systemArtifacts.get(depKey);
                if (resolvedDep == null) {
                    resolvedDep = pomFinder.findPomForArtifact(
                            dep.getGroupId(), dep.getArtifactId());
                    if (resolvedDep == null) {
                        logger.warn("Skipping not found dependency: {}", depKey);
                        skippedDependencies.add(depKey);
                        continue;
                    }
                    systemArtifacts.put(depKey, resolvedDep);
                }

                resolvedDep = resolvedDep.toBuilder()
                        .testContext(current.isTestContext())
                        .build();

                systemArtifacts.put(depKey, resolvedDep);


                if (!processedArtifacts.contains(depKey)) {
                    processedArtifacts.add(depKey);
                    queue.add(resolvedDep);
                }
            }
        }
        return skippedDependencies;
    }

    private Set<String> filterStandardConfigurations(Set<String> configNames) {
        if (configNames == null || configNames.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> filtered = new HashSet<>();
        for (String name : configNames) {
            if (isStandardConfigurationName(name)) {
                filtered.add(name);
            }
        }
        return filtered;
    }

    private boolean isStandardConfigurationName(String name) {
        if (name == null) {
            return false;
        }
        String n = name.trim().toLowerCase(Locale.ROOT);
        switch (n) {
            case "api":
            case "implementation":
            case "compileonly":
            case "compileonlyapi":
            case "runtimeonly":
            case "runtime":
            case "annotationprocessor":
            case "testimplementation":
            case "testcompileonly":
            case "testruntimeonly":
            case "testannotationprocessor":
                return true;
            default:
                return false;
        }
    }
}
