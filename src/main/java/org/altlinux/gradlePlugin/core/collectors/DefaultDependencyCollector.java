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
package org.altlinux.gradlePlugin.core.collectors;

import org.altlinux.gradlePlugin.api.DependencyCollector;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.invocation.Gradle;

import java.util.*;

/**
 * Collects declared project dependencies across all configurations and projects.
 */
public class DefaultDependencyCollector implements DependencyCollector {
    private final Map<String, Set<String>> requestedVersions = new HashMap<>();
    private final Set<String> dependencies = new LinkedHashSet<>();

    /**
     * Collects dependencies from all projects and configurations.
     *
     * @param gradle the current Gradle instance
     * @return set of collected dependency keys (group:artifact)
     */
    public Set<String> collect(Gradle gradle) {
        gradle.allprojects(p -> p.getConfigurations().all(cfg -> {
            for (Dependency d : cfg.getDependencies()) {
                if (d.getGroup() != null && d.getName() != null) {
                    String key = d.getGroup() + ":" + d.getName();
                    dependencies.add(key);
                    requestedVersions.computeIfAbsent(key, k -> new HashSet<>()).add(d.getVersion());
                }
            }
        }));
        return dependencies;
    }

    public Map<String, Set<String>> getRequestedVersions() {
        return requestedVersions;
    }
}