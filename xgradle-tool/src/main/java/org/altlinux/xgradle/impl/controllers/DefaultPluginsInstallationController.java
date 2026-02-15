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

package org.altlinux.xgradle.impl.controllers;

import com.beust.jcommander.JCommander;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.controllers.ArtifactsInstallationController;
import org.altlinux.xgradle.interfaces.installers.ArtifactsInstaller;
import org.altlinux.xgradle.impl.cli.CliArgumentsContainer;
import org.altlinux.xgradle.impl.enums.ProcessingType;

import org.slf4j.Logger;

import static org.altlinux.xgradle.impl.cli.CliPreconditions.require;

/**
 * Controller for managing Gradle plugin installation process.
 * Implements {@link ArtifactsInstallationController}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultPluginsInstallationController implements ArtifactsInstallationController {

    private final ArtifactsInstaller pluginArtifactsInstaller;

    @Inject
    DefaultPluginsInstallationController(ArtifactsInstaller pluginArtifactsInstaller) {
        this.pluginArtifactsInstaller = pluginArtifactsInstaller;
    }

    @Override
    public void configurePluginArtifactsInstallation(
            JCommander jCommander,
            String[] args,
            CliArgumentsContainer arguments,
            Logger logger
    ) {
        if (!arguments.hasInstallPluginParameter()) {
            return;
        }

        require(arguments.hasSearchingDirectory(), "No searching directory specified");
        require(arguments.hasArtifactName(), "Please specify an artifact name. (--artifact=<artifactName>)");
        require(arguments.hasPomInstallationDirectory(), "No POM installation directory specified");
        require(arguments.hasJarInstallationDirectory(), "No JAR installation directory specified");

        pluginArtifactsInstaller.install(
                arguments.getSearchingDirectory(),
                arguments.getArtifactName(),
                arguments.getPomInstallationDirectory(),
                arguments.getJarInstallationDirectory(),
                ProcessingType.PLUGINS
        );
    }
}
