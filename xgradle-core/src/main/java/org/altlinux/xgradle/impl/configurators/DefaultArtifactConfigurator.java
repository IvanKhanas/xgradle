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
package org.altlinux.xgradle.impl.configurators;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.altlinux.xgradle.interfaces.configurators.ArtifactConfigurator;
import org.altlinux.xgradle.interfaces.managers.ScopeManager;
import org.altlinux.xgradle.impl.enums.ConfigurationType;
import org.altlinux.xgradle.impl.enums.MavenPackaging;
import org.altlinux.xgradle.impl.enums.MavenScope;
import org.altlinux.xgradle.impl.model.ConfigurationInfo;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.invocation.Gradle;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
/**
 * Configurator for Artifact.
 * Implements {@link ArtifactConfigurator}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

@Singleton
final class DefaultArtifactConfigurator implements ArtifactConfigurator {

    private final ScopeManager scopeManager;

    private final Map<String, Set<String>> configurationArtifacts = new LinkedHashMap<>();

    @Inject
    DefaultArtifactConfigurator(ScopeManager scopeManager) {
        this.scopeManager = scopeManager;
    }

    @Override
    public void configure(
            Gradle gradle,
            Map<String, MavenCoordinate> systemArtifacts,
            Map<String, Set<String>> dependencyConfigNames,
            Map<String, Set<ConfigurationInfo>> dependencyConfigurations,
            Set<String> testContextDependencies,
            Map<String, MavenScope> dependencyScopes
    ) {
        if (gradle == null || systemArtifacts == null || systemArtifacts.isEmpty()) {
            return;
        }

        gradle.allprojects(project -> addArtifactsToProject(
                project,
                systemArtifacts,
                dependencyConfigNames,
                dependencyConfigurations,
                testContextDependencies,
                dependencyScopes
        ));
    }

    private void addArtifactsToProject(
            Project project,
            Map<String, MavenCoordinate> systemArtifacts,
            Map<String, Set<String>> dependencyConfigNames,
            Map<String, Set<ConfigurationInfo>> dependencyConfigurations,
            Set<String> testContextDependencies,
            Map<String, MavenScope> dependencyScopes
    ) {
        for (Map.Entry<String, MavenCoordinate> e : systemArtifacts.entrySet()) {
            String key = e.getKey();
            MavenCoordinate coord = e.getValue();

            if (key == null || coord == null) {
                continue;
            }

            if (shouldSkip(coord)) {
                continue;
            }

            if (isSelfDependency(project, coord)) {
                continue;
            }

            Set<String> originalConfigs = dependencyConfigNames != null ? dependencyConfigNames.get(key) : null;
            if (originalConfigs != null && !originalConfigs.isEmpty()) {
                addToOriginalConfigurations(project, key, coord, originalConfigs);
            } else {
                addByDerivedConfiguration(
                        project,
                        key,
                        coord,
                        dependencyConfigurations,
                        testContextDependencies,
                        dependencyScopes
                );
            }
        }
    }

    private boolean shouldSkip(MavenCoordinate coord) {
        if (coord.isBom()) {
            return true;
        }
        String packaging = coord.getPackaging();
        return packaging != null && MavenPackaging.POM.getPackaging().equals(packaging);
    }

    private void addToOriginalConfigurations(
            Project project,
            String key,
            MavenCoordinate coord,
            Set<String> configNames
    ) {
        String version = coord.getVersion();
        if (version == null || version.isBlank()) {
            return;
        }

        String notation = key + ":" + version;

        for (String configName : configNames) {
            if (configName == null || configName.isBlank()) {
                continue;
            }
            safeAddToConfiguration(project, configName, notation);
        }
    }

    private void addByDerivedConfiguration(
            Project project,
            String key,
            MavenCoordinate coord,
            Map<String, Set<ConfigurationInfo>> dependencyConfigurations,
            Set<String> testContextDependencies,
            Map<String, MavenScope> dependencyScopes
    ) {
        String version = coord.getVersion();
        if (version == null || version.isBlank()) {
            return;
        }

        String notation = key + ":" + version;

        if ((testContextDependencies != null && testContextDependencies.contains(key)) || coord.isTestContext()) {
            safeAddToConfiguration(project, ConfigurationType.TEST.gradleConfiguration(), notation);
            return;
        }

        ConfigurationType type = determineConfigurationType(key, dependencyConfigurations);
        if (type != null && type != ConfigurationType.UNKNOWN) {
            safeAddToConfiguration(project, type.gradleConfiguration(), notation);
            return;
        }

        addBasedOnScopeDefault(project, key, notation, dependencyScopes);
    }

    private ConfigurationType determineConfigurationType(
            String depKey,
            Map<String, Set<ConfigurationInfo>> dependencyConfigurations
    ) {
        if (dependencyConfigurations == null) {
            return null;
        }

        Set<ConfigurationInfo> infos = dependencyConfigurations.get(depKey);
        if (infos == null || infos.isEmpty()) {
            return null;
        }

        for (ConfigurationInfo info : infos) {
            if (info == null) {
                continue;
            }
            if (info.hasTestConfiguration()) {
                continue;
            }

            ConfigurationType t = info.getType();
            if (t != null && t != ConfigurationType.UNKNOWN) {
                return t;
            }
        }

        return null;
    }

    private void addBasedOnScopeDefault(
            Project project,
            String key,
            String notation,
            Map<String, MavenScope> dependencyScopes
    ) {
        MavenScope scope = scopeManager.getScope(dependencyScopes, key);

        if (scope == MavenScope.PROVIDED || scope == MavenScope.COMPILE) {
            safeAddToConfiguration(project, ConfigurationType.COMPILE_ONLY.gradleConfiguration(), notation);
            return;
        }

        if (scope == MavenScope.RUNTIME) {
            safeAddToConfiguration(project, ConfigurationType.RUNTIME.gradleConfiguration(), notation);
            return;
        }

        if (scope == MavenScope.TEST) {
            safeAddToConfiguration(project, ConfigurationType.TEST.gradleConfiguration(), notation);
            return;
        }

        safeAddToConfiguration(project, ConfigurationType.IMPLEMENTATION.gradleConfiguration(), notation);
    }

    private void safeAddToConfiguration(Project project, String configName, String notation) {
        Configuration config = project.getConfigurations().findByName(configName);
        if (config == null || !canModifyConfiguration(config)) {
            return;
        }

        try {
            project.getDependencies().add(configName, notation);
            trackArtifact(configName, notation);
        } catch (Exception e) {
            project.getLogger().debug("Cannot add to configuration '{}': {}", configName, e.getMessage());
        }
    }

    private boolean canModifyConfiguration(Configuration configuration) {
        return configuration.getState() != Configuration.State.RESOLVED;
    }

    private void trackArtifact(String configName, String notation) {
        configurationArtifacts
                .computeIfAbsent(configName, k -> new LinkedHashSet<>())
                .add(notation);
    }

    private boolean isSelfDependency(Project project, MavenCoordinate coord) {
        if (coord.getGroupId() == null || coord.getArtifactId() == null) {
            return false;
        }

        Map<String, Project> projectIndex = new HashMap<>();
        Project root = project.getRootProject();
        for (Project p : root.getAllprojects()) {
            Object group = p.getGroup();
            String name = p.getName();
            if (group != null && name != null) {
                projectIndex.put(group + ":" + name, p);
            }
        }

        String coordKey = coord.getGroupId() + ":" + coord.getArtifactId();
        boolean isSelf = projectIndex.containsKey(coordKey);

        if (isSelf) {
            project.getLogger().debug(
                    "Detected project dependency, skipping: {}:{}:{}",
                    coord.getGroupId(),
                    coord.getArtifactId(),
                    coord.getVersion()
            );
        }

        return isSelf;
    }


    @Override
    public Map<String, Set<String>> getConfigurationArtifacts() {
        return configurationArtifacts;
    }
}
