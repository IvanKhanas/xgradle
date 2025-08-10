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
package org.altlinux.gradlePlugin.core;

import org.altlinux.gradlePlugin.model.MavenCoordinate;

import org.gradle.api.artifacts.DependencySubstitutions;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.invocation.Gradle;
import org.gradle.util.internal.VersionNumber;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DependencySubstitutor {
    private final Map<String, Set<String>> requestedVersions;
    private final Map<String, MavenCoordinate> systemArtifacts;
    private final Map<String, String> overrideLogs = new ConcurrentHashMap<>();
    private final Map<String, String> applyLogs = new ConcurrentHashMap<>();

    public DependencySubstitutor(
            Map<String, Set<String>> requestedVersions,
            Map<String, MavenCoordinate> systemArtifacts
    ) {
        this.requestedVersions = requestedVersions;
        this.systemArtifacts = systemArtifacts;
    }

    public void configure(Gradle gradle) {
        gradle.allprojects(project -> project.getConfigurations()
                .matching(config ->
                        config.getName().endsWith("Implementation") ||
                                "compileClasspath".equals(config.getName())
                )
                .all(config -> config.getResolutionStrategy()
                        .dependencySubstitution(this::applySubstitutions)
                ));
    }

    private void applySubstitutions(DependencySubstitutions substitutions) {
        substitutions.all(details -> {
            if (!(details.getRequested() instanceof ModuleComponentSelector)) return;

            ModuleComponentSelector sel = (ModuleComponentSelector) details.getRequested();
            String key = sel.getGroup() + ":" + sel.getModule();
            MavenCoordinate sys = systemArtifacts.get(key);
            if (sys == null || sys.isBom()) return;

            details.useTarget(
                    substitutions.module(key + ":" + sys.getVersion()),
                    "System dependency override"
            );

            logSubstitution(key, sys.getVersion(), resolveOriginalVersion(key, sel.getVersion()));
        });
    }

    private String resolveOriginalVersion(String key, String requestedVersion) {
        Set<String> versions = requestedVersions.get(key);
        if (versions != null && !versions.isEmpty()) {
            return versions.stream()
                    .filter(Objects::nonNull)
                    .max(Comparator.comparing(VersionNumber::parse))
                    .orElse(requestedVersion);
        }
        return requestedVersion != null ? requestedVersion : "(unspecified)";
    }

    private void logSubstitution(String key, String newVersion, String originalVersion) {
        String logKey = key + ":" + newVersion;
        if (newVersion.equals(originalVersion)) {
            applyLogs.putIfAbsent(logKey, "Apply version: " + key + ":" + newVersion);
        } else {
            overrideLogs.put(logKey, "Override version: " + key + ":" + originalVersion + " -> " + newVersion);
        }
    }

    public Map<String, String> getOverrideLogs() {
        return overrideLogs;
    }

    public Map<String, String> getApplyLogs() {
        return applyLogs;
    }
}