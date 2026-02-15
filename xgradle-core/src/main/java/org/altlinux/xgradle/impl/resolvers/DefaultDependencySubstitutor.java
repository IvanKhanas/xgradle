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
package org.altlinux.xgradle.impl.resolvers;

import com.google.inject.Singleton;
import org.altlinux.xgradle.interfaces.resolvers.DependencySubstitutor;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.gradle.api.artifacts.DependencySubstitutions;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.invocation.Gradle;
import org.gradle.util.internal.VersionNumber;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Handles dependency version substitutions during Gradle resolution.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
public final class DefaultDependencySubstitutor implements DependencySubstitutor {

    @Override
    public void configure(
            Gradle gradle,
            Map<String, Set<String>> requestedVersions,
            Map<String, MavenCoordinate> systemArtifacts,
            Map<String, String> managedVersions,
            Map<String, String> overrideLogs,
            Map<String, String> applyLogs
    ) {
        if (gradle == null) {
            return;
        }

        Map<String, Set<String>> requested = requestedVersions != null ? requestedVersions : Map.of();
        Map<String, MavenCoordinate> artifacts = systemArtifacts != null ? systemArtifacts : Map.of();
        Map<String, String> managed = managedVersions != null ? managedVersions : Map.of();

        Map<String, String> overrides = overrideLogs;
        Map<String, String> applies = applyLogs;
        if (overrides == null || applies == null) {
            return;
        }

        gradle.allprojects(project -> project.getConfigurations()
                .all(config -> config.getResolutionStrategy()
                        .dependencySubstitution(substitutions ->
                                applySubstitutions(substitutions, requested, artifacts, managed, overrides, applies))
                )
        );
    }

    private void applySubstitutions(
            DependencySubstitutions substitutions,
            Map<String, Set<String>> requestedVersions,
            Map<String, MavenCoordinate> systemArtifacts,
            Map<String, String> managedVersions,
            Map<String, String> overrideLogs,
            Map<String, String> applyLogs
    ) {
        substitutions.all(details -> {
            if (!(details.getRequested() instanceof ModuleComponentSelector)) {
                return;
            }

            ModuleComponentSelector sel = (ModuleComponentSelector) details.getRequested();
            String key = sel.getGroup() + ":" + sel.getModule();
            String originalVersion = resolveOriginalVersion(requestedVersions, key, sel.getVersion());
            String newVersion = null;
            boolean isOverride = false;
            boolean isBomApply = false;

            MavenCoordinate system = systemArtifacts.get(key);
            if (system != null) {
                newVersion = system.getVersion();
                isOverride = newVersion != null && !newVersion.equals(originalVersion);
            } else if (managedVersions.containsKey(key)) {
                newVersion = managedVersions.get(key);
                isBomApply = newVersion != null && !newVersion.equals(originalVersion);
            }

            if (isOverride) {
                String logMessage = "Override version: " + key + ":" + originalVersion + " -> " + newVersion;
                overrideLogs.put(key + "|" + originalVersion + "|" + newVersion, logMessage);
                details.useTarget(
                        substitutions.module(key + ":" + newVersion),
                        "System dependency override"
                );
            } else if (isBomApply) {
                String logMessage = "Apply BOM version: " + key + ":" + newVersion;
                applyLogs.put(key, logMessage);
                details.useTarget(
                        substitutions.module(key + ":" + newVersion),
                        "BOM managed version"
                );
            }
        });
    }

    private String resolveOriginalVersion(
            Map<String, Set<String>> requestedVersions,
            String key,
            String requestedVersion
    ) {
        Set<String> versions = requestedVersions.get(key);
        if (versions != null && !versions.isEmpty()) {
            return versions.stream()
                    .filter(Objects::nonNull)
                    .max(Comparator.comparing(VersionNumber::parse))
                    .orElse(requestedVersion);
        }
        return requestedVersion != null ? requestedVersion : "(unspecified)";
    }
}
