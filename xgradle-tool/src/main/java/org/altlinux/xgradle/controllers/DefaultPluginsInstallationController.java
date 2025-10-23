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
package org.altlinux.xgradle.controllers;

import com.beust.jcommander.JCommander;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.ExitCode;
import org.altlinux.xgradle.ProcessingType;
import org.altlinux.xgradle.api.controllers.ArtifactsInstallationController;
import org.altlinux.xgradle.api.installers.ArtifactsInstaller;
import org.altlinux.xgradle.cli.CliArgumentsContainer;

import org.slf4j.Logger;

/**
 * Controller for managing Gradle plugin installation process.
 * Handles command-line configuration and validation for plugin installation.
 *
 * @author Ivan Khanas
 */
@Singleton
public class DefaultPluginsInstallationController implements ArtifactsInstallationController {
    private final ArtifactsInstaller pluginArtifactsInstaller;

    /**
     * Constructs a new DefaultPluginsInstallationController with required dependencies.
     *
     * @param pluginArtifactsInstaller installer for plugin artifacts
     */
    @Inject
    public DefaultPluginsInstallationController(ArtifactsInstaller pluginArtifactsInstaller) {
        this.pluginArtifactsInstaller = pluginArtifactsInstaller;
    }

    /**
     * Configures and validates plugin artifacts installation based on command-line arguments.
     * Performs parameter validation and executes installation if all requirements are met.
     *
     * @param jCommander the command-line parser
     * @param args command-line arguments
     * @param arguments parsed command-line arguments container
     * @param logger logger for error and information messages
     */
    @Override
    public void configurePluginArtifactsInstallation(JCommander jCommander,
                                                     String[] args,
                                                     CliArgumentsContainer arguments,
                                                     Logger logger)
    {
        if(arguments.hasInstallPluginParameter()) {
            if(arguments.hasSearchingDirectory()) {
                if(arguments.hasArtifactName()) {
                    if(arguments.hasPomInstallationDirectory()){
                        if(arguments.hasJarInstallationDirectory()) {
                            pluginArtifactsInstaller.install(
                                    arguments.getSearchingDirectory(),
                                    arguments.getArtifactName(),
                                    arguments.getPomInstallationDirectory(),
                                    arguments.getJarInstallationDirectory(),
                                    ProcessingType.PLUGINS
                            );
                        }else {
                            logger.error("No Jar installation directory specified");
                            jCommander.usage();
                            ExitCode.ERROR.exit();
                        }
                    }else {
                        logger.error("No POM installation directory specified");
                        jCommander.usage();
                        ExitCode.ERROR.exit();
                    }
                }else {
                    logger.error("Please specify an artifact name. (--artifact=<artifactName>)");
                    jCommander.usage();
                    ExitCode.ERROR.exit();
                }
            }else {
                logger.error("No searching directory specified");
                jCommander.usage();
                ExitCode.ERROR.exit();
            }
        }
    }
}
