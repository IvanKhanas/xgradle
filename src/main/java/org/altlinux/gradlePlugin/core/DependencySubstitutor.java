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

/**
 * Handles dependency version substitutions during Gradle resolution.
 *
 * <p>This class performs two critical operations in the dependency resolution process:
 * <ol>
 *   <li><strong>Version Overrides</strong> - Replaces requested dependency versions
 *       with versions from system artifacts</li>
 *   <li><strong>BOM Application</strong> - Applies versions from Bill-of-Materials (BOM)
 *       dependencies</li>
 * </ol>
 *
 * <p>Key features:
 * <ul>
 *   <li>Integrates with Gradle's {@link DependencySubstitutions} API</li>
 *   <li>Maintains detailed audit logs of version changes</li>
 *   <li>Handles version conflict resolution using the highest version strategy</li>
 *   <li>Supports parallel execution via thread-safe collections</li>
 * </ul>
 *
 * <p>Workflow:
 * <pre>
 * 1. Configure substitution rules for all projects
 * 2. During resolution:
 *    a. Check if dependency has system version
 *    b. Compare requested vs system version
 *    c. Apply override if versions differ
 *    d. Track BOM-managed dependencies
 * 3. Provide access to operation logs
 * </pre>
 *
 * @see MavenCoordinate
 *
 * @author Ivan Khanas
 */
public class DependencySubstitutor {
    private final Map<String, Set<String>> requestedVersions;
    private final Map<String, MavenCoordinate> systemArtifacts;
    private final Map<String, String> managedVersions;
    private final Map<String, String> overrideLogs = new ConcurrentHashMap<>();
    private final Map<String, String> applyLogs = new ConcurrentHashMap<>();

    /**
     * Constructs a new DependencySubstitutor instance.
     *
     * @param requestedVersions Map of dependency keys to requested versions
     *        (format: {@code "group:artifact" -> Set["version1", "version2"]})
     * @param systemArtifacts Resolved system artifacts mapped by
     *        their coordinates (format: {@code "group:artifact" -> MavenCoordinate})
     *        (format: {@code "group:artifact"})
     */
    public DependencySubstitutor(
            Map<String, Set<String>> requestedVersions,
            Map<String, MavenCoordinate> systemArtifacts,
            Map<String, String> managedVersions) {
        this.requestedVersions = requestedVersions;
        this.systemArtifacts = systemArtifacts;
        this.managedVersions = managedVersions;
    }

    /**
     * Configures dependency substitution rules for all projects and configurations.
     *
     * <p>This method:
     * <ul>
     *   <li>Applies to all configurations in all projects</li>
     *   <li>Registers the {@link #applySubstitutions} handler</li>
     *   <li>Must be called during Gradle's configuration phase</li>
     * </ul>
     *
     * @param gradle The Gradle instance to configure
     */
    public void configure(Gradle gradle) {
        gradle.allprojects(project -> project.getConfigurations()
                .all(config -> config.getResolutionStrategy()
                        .dependencySubstitution(this::applySubstitutions)
                )
        );
    }

    /**
     * Applies substitution rules to individual dependencies.
     *
     * <p>Internal handler that performs:
     * <ul>
     *   <li>Version override when system artifact exists</li>
     *   <li>BOM version application tracking</li>
     *   <li>Detailed logging of version changes</li>
     * </ul>
     *
     * <p>Note: Automatically called by Gradle's dependency resolution engine
     *
     * @param substitutions Gradle's dependency substitutions context
     */
    private void applySubstitutions(DependencySubstitutions substitutions) {
        substitutions.all(details -> {
            if (!(details.getRequested() instanceof ModuleComponentSelector)) return;

            ModuleComponentSelector sel = (ModuleComponentSelector) details.getRequested();
            String key = sel.getGroup() + ":" + sel.getModule();
            String originalVersion = resolveOriginalVersion(key, sel.getVersion());
            String newVersion = null;
            boolean isOverride = false;
            boolean isBomApply = false;

            if (systemArtifacts.containsKey(key)) {
                newVersion = systemArtifacts.get(key).getVersion();
                isOverride = !newVersion.equals(originalVersion);
            } else if (managedVersions.containsKey(key)) {
                newVersion = managedVersions.get(key);
                isBomApply = !newVersion.equals(originalVersion);
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

    /**
     * Resolves the original requested version for a dependency.
     *
     * <p>Version resolution logic:
     * <ol>
     *   <li>If multiple versions requested: returns highest semantic version</li>
     *   <li>If single version: returns that version</li>
     *   <li>If no version specified: returns "(unspecified)"</li>
     * </ol>
     *
     * @param key Dependency key (format: "group:artifact")
     * @param requestedVersion The version string from dependency declaration
     *
     * @return Resolved original version string
     */
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

    /**
     * Gets the version override audit log.
     *
     * <p>Log format:
     * <pre>
     * Key: "group:artifact|requestedVersion|newVersion"
     * Value: "Override version: group:artifact:requestedVersion -> newVersion"
     * </pre>
     *
     * @return Map of version override records
     */
    public Map<String, String> getOverrideLogs() {
        return overrideLogs;
    }

    /**
     * Gets the BOM version application log.
     *
     * <p>Log format:
     * <pre>
     * Key: "group:artifact"
     * Value: "Apply BOM version: group:artifact:version"
     * </pre>
     *
     * @return Map of BOM version applications
     */
    public Map<String, String> getApplyLogs() {
        return applyLogs;
    }
}