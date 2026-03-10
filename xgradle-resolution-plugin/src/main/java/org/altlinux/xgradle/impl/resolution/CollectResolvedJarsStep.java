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
package org.altlinux.xgradle.impl.resolution;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.altlinux.xgradle.interfaces.configurators.ArtifactConfigurator;
import org.altlinux.xgradle.interfaces.resolution.Order;
import org.altlinux.xgradle.interfaces.resolution.ResolutionStep;
import org.altlinux.xgradle.interfaces.resolution.ResolvedArtifactsRegistry;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.logging.Logger;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * Registers listeners to capture resolved JAR files used by configurations.
 * Implements {@link ResolutionStep}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
@Order(1100)
final class CollectResolvedJarsStep implements ResolutionStep {

    private final ArtifactConfigurator artifactConfigurator;

    @Inject
    CollectResolvedJarsStep(ArtifactConfigurator artifactConfigurator) {
        this.artifactConfigurator = artifactConfigurator;
    }

    @Override
    public String name() {
        return "collect-resolved-jars";
    }

    @Override
    public void execute(ResolutionContext resolutionContext) {
        Map<String, Set<String>> configurationArtifacts =
                artifactConfigurator.getConfigurationArtifacts();
        if (configurationArtifacts == null || configurationArtifacts.isEmpty()) {
            return;
        }

        Set<File> resolvedJars = ResolvedArtifactsRegistry.getOrCreate(
                resolutionContext.getGradle()
        );

        for (Project project : resolutionContext.getGradle().getRootProject().getAllprojects()) {
            Logger logger = project.getLogger();
            for (String configName : configurationArtifacts.keySet()) {
                if (configName == null || configName.isBlank()) {
                    continue;
                }

                Configuration configuration = project.getConfigurations().findByName(configName);
                if (configuration == null || !configuration.isCanBeResolved()) {
                    continue;
                }

                configuration.getIncoming().afterResolve(resolvable -> {
                    try {
                        for (ResolvedArtifact artifact
                                : configuration.getResolvedConfiguration().getResolvedArtifacts()) {
                            File file = artifact.getFile();
                            if (file != null && file.isFile() && isJar(file)) {
                                resolvedJars.add(file);
                            }
                        }
                    } catch (RuntimeException e) {
                        logger.debug("Failed to collect resolved jars for '{}': {}", configName, e.getMessage());
                    }
                });
            }
        }
    }

    private boolean isJar(File file) {
        String name = file.getName();
        return name.endsWith(".jar");
    }
}
