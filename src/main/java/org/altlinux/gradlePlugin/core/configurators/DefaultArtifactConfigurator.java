package org.altlinux.gradlePlugin.core.configurators;

import org.altlinux.gradlePlugin.api.ArtifactConfigurator;
import org.altlinux.gradlePlugin.core.collectors.info.ConfigurationInfo;
import org.altlinux.gradlePlugin.core.managers.ScopeManager;
import org.altlinux.gradlePlugin.model.MavenCoordinate;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.invocation.Gradle;

import java.util.*;

public class DefaultArtifactConfigurator implements ArtifactConfigurator {
    private final ScopeManager scopeManager;
    private final Map<String, Set<String>> configurationArtifacts = new HashMap<>();
    private final Map<String, Set<ConfigurationInfo>> dependencyConfigurations;
    private final Set<String> testContextDependencies;

    public DefaultArtifactConfigurator(
            ScopeManager scopeManager,
            Map<String, Set<ConfigurationInfo>> dependencyConfigurations,
            Set<String> testContextDependencies) {
        this.scopeManager = scopeManager;
        this.dependencyConfigurations = dependencyConfigurations;
        this.testContextDependencies = testContextDependencies; }

    @Override
    public void configure(Gradle gradle,
                          Map<String, MavenCoordinate> systemArtifacts,
                          Map<String, Set<String>> dependencyConfigNames) {

        gradle.allprojects(proj -> {
            configurationArtifacts.clear();
            systemArtifacts.forEach((key, coord) -> {
                if (shouldSkip(coord)) return;

                Set<String> configNames = dependencyConfigNames.get(key);
                if (configNames != null && !configNames.isEmpty()) {
                    addToOriginalConfigurations(proj, key, coord, configNames);
                } else {
                    addBasedOnScope(proj, key, coord);
                }
            });
        });
    }

    private boolean shouldSkip(MavenCoordinate coord) {
        return coord.isBom() || "pom".equals(coord.getPackaging());
    }

    private void addToOriginalConfigurations(Project project,
                                             String key,
                                             MavenCoordinate coord,
                                             Set<String> configNames) {

        String notation = key + ":" + coord.getVersion();
        for (String configName : configNames) {
            Configuration config = project.getConfigurations().findByName(configName);
            if (config != null) {
                project.getDependencies().add(configName, notation);
                trackArtifact(configName, notation);
            }
        }
    }

    private void addBasedOnScope(Project project, String key, MavenCoordinate coord) {
        String notation = key + ":" + coord.getVersion();

        if (testContextDependencies.contains(key) || coord.isTestContext()) {
            addToTestConfiguration(project, notation);
            return;
        }

        String type = determineConfigurationType(key);

        if (type != null) {
            switch (type) {
                case "API":
                    project.getDependencies().add("api", notation);
                    trackArtifact("api", notation);
                    break;
                case "IMPLEMENTATION":
                    project.getDependencies().add("implementation", notation);
                    trackArtifact("implementation", notation);
                    break;
                case "RUNTIME":
                    project.getDependencies().add("runtimeOnly", notation);
                    trackArtifact("runtimeOnly", notation);
                    break;
                case "COMPILE_ONLY":
                    project.getDependencies().add("compileOnly", notation);
                    trackArtifact("compileOnly", notation);
                case "TEST":
                    addToTestConfiguration(project, notation);
                    break;
                default:
                    addBasedOnScopeDefault(project, key, notation);
            }
        } else {
            addBasedOnScopeDefault(project, key, notation);
        }
    }

    private String determineConfigurationType(String key) {
        if (dependencyConfigurations.containsKey(key)) {
            for (ConfigurationInfo configInfo : dependencyConfigurations.get(key)) {
                if (!configInfo.isTestConfigutation()) {
                    return configInfo.getType();
                }
            }
        }
        return null;
    }

    private void addToTestConfiguration(Project project, String notation) {
        project.getDependencies().add("testImplementation", notation);
        trackArtifact("testImplementation", notation);
    }

    private void addBasedOnScopeDefault(Project project, String key, String notation) {
        String scope = scopeManager.getScope(key);
        if ("provided".equals(scope) || "compileOnly".equals(scope)) {
            project.getDependencies().add("compileOnly", notation);
            trackArtifact("compileOnly", notation);
        } else if ("runtime".equals(scope) || "runtimeOnly".equals(scope)) {
            project.getDependencies().add("runtimeOnly", notation);
            trackArtifact("runtimeOnly", notation);
        } else {
            project.getDependencies().add("implementation", notation);
            trackArtifact("implementation", notation);
        }
    }

    private void trackArtifact(String config, String artifact) {
        configurationArtifacts
                .computeIfAbsent(config, k -> new LinkedHashSet<>())
                .add(artifact);
    }

    public Map<String, Set<String>> getConfigurationArtifacts() {
        return configurationArtifacts;
    }
}   
