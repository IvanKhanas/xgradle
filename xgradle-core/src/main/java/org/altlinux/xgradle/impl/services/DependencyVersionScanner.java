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
package org.altlinux.xgradle.impl.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.maven.PomFinder;
import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.altlinux.xgradle.interfaces.services.ArtifactVerifier;
import org.altlinux.xgradle.interfaces.services.VersionScanner;
import org.altlinux.xgradle.impl.enums.MavenScope;
import org.altlinux.xgradle.impl.model.MavenCoordinate;

import java.util.*;

/**
 * Scans system artifacts to resolve Maven coordinates for project dependencies, including Gradle plugins and dependencies.
 * Implements {@link VersionScanner}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
class DependencyVersionScanner implements VersionScanner {

    private final PomFinder pomFinder;
    private final PomParser pomParser;
    private final ArtifactVerifier artifactVerifier;
    private final Set<String> notFoundDependencies = new HashSet<>();

    @Inject
    DependencyVersionScanner(
            PomFinder pomFinder,
            ArtifactVerifier artifactVerifier,
            PomParser pomParser
    ) {
        this.pomFinder = pomFinder;
        this.pomParser = pomParser;
        this.artifactVerifier = artifactVerifier;
    }

    public Map<String, MavenCoordinate> scanSystemArtifacts(Set<String> projectDependencies) {
        notFoundDependencies.clear();
        Map<String, MavenCoordinate> versions = new HashMap<>();
        Set<String> processedDependencies = new HashSet<>();
        Queue<String> dependencyQueue = new LinkedList<>(projectDependencies);

        while (!dependencyQueue.isEmpty()) {
            String dep = dependencyQueue.poll();
            if (processedDependencies.contains(dep)) continue;
            processedDependencies.add(dep);
            if (versions.containsKey(dep)) continue;

            if (dep.endsWith(".gradle.plugin")) {
                resolveGradlePlugin(dep, versions);
            } else {
                resolveRegularDependency(dep, versions);
            }

            MavenCoordinate coord = versions.get(dep);
            if (coord != null && coord.getPomPath() != null) {
                scanProvidedDependencies(coord, dependencyQueue, versions);
            }
        }
        return versions;
    }

    private void scanProvidedDependencies(MavenCoordinate parentCoord,
                                          Queue<String> dependencyQueue,
                                          Map<String, MavenCoordinate> versions) {
        List<MavenCoordinate> dependencies = pomParser
                .parseDependencies(parentCoord.getPomPath());

        for (MavenCoordinate dep : dependencies) {
            if (MavenScope.PROVIDED.equals(dep.getScope()) || MavenScope.RUNTIME.equals(dep.getScope())) {
                String depKey = dep.getGroupId() + ":" + dep.getArtifactId();
                if (!versions.containsKey(depKey) && !dependencyQueue.contains(depKey)) {
                    dependencyQueue.add(depKey);
                }
            }
        }
    }

    private void resolveGradlePlugin(String pluginDep, Map<String, MavenCoordinate> versions) {
        String[] parts = pluginDep.split(":");
        if (parts.length != 2) return;

        MavenCoordinate pom = findPluginArtifact(parts[0]);
        if (pom != null && artifactVerifier.verifyArtifactExists(pom)) {
            versions.put(pluginDep, pom);
        }
    }

    private void resolveRegularDependency(String dep, Map<String, MavenCoordinate> versions) {
        String[] parts = dep.split(":");
        if (parts.length < 2) return;

        String groupId = parts[0];
        String artifactId = parts[1];

        if (hasPlaceholder(groupId) || hasPlaceholder(artifactId)) return;

        MavenCoordinate pom = pomFinder.findPomForArtifact(groupId, artifactId);
        if (pom == null) {
            notFoundDependencies.add(dep);
            return;
        }
        if (!artifactVerifier.verifyArtifactExists(pom)) {
            notFoundDependencies.add(dep);
            return;
        }
        versions.put(dep, pom);
    }

    private boolean hasPlaceholder(String value) {
        return value != null && value.contains("${");
    }

    @Override
    public MavenCoordinate findPluginArtifact(String pluginId) {
        String baseName = pluginId.contains(".") ?
                pluginId.substring(pluginId.lastIndexOf('.') + 1) : pluginId;

        String[] artifactIds = {
                pluginId + ".gradle.plugin", pluginId,
                baseName + "-plugin", "gradle-" + baseName,
                "gradle-" + baseName + "-plugin", baseName + "-gradle-plugin",
                "gradle-plugin-" + baseName, baseName + "-gradle"
        };

        for (String artifactId : artifactIds) {
            MavenCoordinate coord = pomFinder.findPomForArtifact(pluginId, artifactId);
            if (coord != null && artifactVerifier.verifyArtifactExists(coord)) {
                return coord;
            }
        }

        if (pluginId.contains(".")) {
            String withoutDomain = pluginId.substring(pluginId.indexOf('.') + 1).replace('.', '-');
            String[] extendedArtifactIds = {
                    withoutDomain, withoutDomain + "-plugin", "gradle-" + withoutDomain,
                    "gradle-" + withoutDomain + "-plugin", withoutDomain + "-gradle-plugin"
            };

            for (String artifactId : extendedArtifactIds) {
                MavenCoordinate coord = pomFinder.findPomForArtifact(pluginId, artifactId);
                if (coord != null && artifactVerifier.verifyArtifactExists(coord)) {
                    return coord;
                }
            }
        }

        return findMainArtifactForGroup(pluginId);
    }

    private MavenCoordinate findMainArtifactForGroup(String groupId) {
        List<MavenCoordinate> candidates = pomFinder.findAllPomsForGroup(groupId);
        return candidates.stream()
                .filter(coord -> coord.getArtifactId().contains("gradle") ||
                        coord.getArtifactId().contains("plugin"))
                .findFirst()
                .orElse(null);
    }

    public Set<String> getNotFoundDependencies() {
        return notFoundDependencies;
    }
}
