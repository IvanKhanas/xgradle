package org.altlinux.gradlePlugin.core.collectors;

import org.altlinux.gradlePlugin.core.collectors.info.ConfigurationInfo;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.invocation.Gradle;

import java.util.*;

public class ConfigurationInfoCollector {
    private final Map<String, Set<ConfigurationInfo>> dependencyConfigurations = new HashMap<>();
    private final Map<String, Boolean> testDependencyFlags = new HashMap<>();
    private final Map<String, Set<String>> dependencyConfigNames = new HashMap<>();

    public void collect(Gradle gradle) {
        gradle.allprojects(project ->
                project.getConfigurations().all(configuration -> {
                    ConfigurationInfo configInfo = new ConfigurationInfo(configuration);
                    for (Dependency dependency : configuration.getDependencies()) {
                        if (dependency.getGroup() != null && dependency.getName() != null) {
                            String key = dependency.getGroup() + ":" + dependency.getName();

                            dependencyConfigurations
                                    .computeIfAbsent(key, k -> new HashSet<>())
                                    .add(configInfo);

                            dependencyConfigNames
                                    .computeIfAbsent(key, k -> new HashSet<>())
                                    .add(configuration.getName());

                            if (configInfo.isTestConfigutation()) {
                                testDependencyFlags.put(key, true);
                            } else {
                                testDependencyFlags.putIfAbsent(key, false);
                            }
                        }
                    }
                })
        );
    }

    public Map<String, Set<ConfigurationInfo>> getDependencyConfigurations() {
        return dependencyConfigurations;
    }

    public Map<String, Boolean> getTestDependencyFlags() {
        return testDependencyFlags;
    }

    public Map<String, Set<String>> getDependencyConfigNames() {
        return dependencyConfigNames;
    }
}