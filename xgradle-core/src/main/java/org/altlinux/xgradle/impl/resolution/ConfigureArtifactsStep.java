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
import org.altlinux.xgradle.interfaces.resolution.ResolutionStep;
import org.altlinux.xgradle.impl.model.ConfigurationInfo;
import org.altlinux.xgradle.impl.model.ConfigurationInfoSnapshot;

import java.util.Map;
import java.util.Set;

/**
 * Configures resolved artifacts into Gradle configurations.
 * Implements {@link ResolutionStep}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class ConfigureArtifactsStep implements ResolutionStep {

    private final ArtifactConfigurator artifactConfigurator;

    @Inject
    ConfigureArtifactsStep(ArtifactConfigurator artifactConfigurator) {
        this.artifactConfigurator = artifactConfigurator;
    }

    @Override
    public String name() {
        return "configure-artifacts";
    }

    @Override
    public void execute(ResolutionContext resolutionContext) {
        ConfigurationInfoSnapshot snapshot = resolutionContext.getConfigurationInfoSnapshot();

        Map<String, Set<String>> dependencyConfigNames =
                resolutionContext.getResolvedConfigNames().isEmpty()
                        ? (snapshot != null ? snapshot.getDependencyConfigNames() : Map.of())
                        : resolutionContext.getResolvedConfigNames();

        Map<String, Set<ConfigurationInfo>> dependencyConfigurations =
                snapshot != null ? snapshot.getDependencyConfigurations() : Map.of();

        artifactConfigurator.configure(
                resolutionContext.getGradle(),
                resolutionContext.getSystemArtifacts(),
                dependencyConfigNames,
                dependencyConfigurations,
                resolutionContext.getTestContextDependencies(),
                resolutionContext.getDependencyScopes()
        );
    }
}
