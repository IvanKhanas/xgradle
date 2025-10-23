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
import org.altlinux.xgradle.api.controllers.XmvnCompatController;
import org.altlinux.xgradle.api.installers.JavadocInstaller;
import org.altlinux.xgradle.cli.CliArgumentsContainer;

import org.slf4j.Logger;

/**
 * Controller for managing Javadoc artifacts installation.
 * Handles command-line configuration and execution of Javadoc installation.
 *
 * @author Ivan Khanas
 */
@Singleton
public class DefaultJavadocXmvnCompatController implements XmvnCompatController {
    private final JavadocInstaller javadocInstaller;

    /**
     * Constructs a new DefaultJavadocXmvnCompatController with required dependencies.
     *
     * @param javadocInstaller installer for Javadoc artifacts
     */
    @Inject
    public DefaultJavadocXmvnCompatController(JavadocInstaller javadocInstaller) {
        this.javadocInstaller = javadocInstaller;
    }

    /**
     * Configures and executes Javadoc artifacts installation.
     * Validates parameters and executes installation if requirements are met.
     *
     * @param jCommander the command-line parser
     * @param args command-line arguments
     * @param arguments parsed command-line arguments container
     * @param logger logger for error and information messages
     */
    @Override
    public void configureXmvnCompatFunctions(JCommander jCommander, String[] args, CliArgumentsContainer arguments, Logger logger) {
        if (arguments.hasJavadocRegistration()) {
            if (arguments.hasSearchingDirectory()) {
                if (arguments.hasJarInstallationDirectory()) {
                    javadocInstaller.installJavadoc(
                            arguments.getSearchingDirectory(),
                            arguments.getArtifactName(),
                            arguments.getJarInstallationDirectory()
                    );
                } else {
                    logger.error("No JAR installation directory specified for Javadoc installation");
                    jCommander.usage();
                    ExitCode.ERROR.exit();
                }
            } else {
                logger.error("No searching directory specified for Javadoc installation");
                jCommander.usage();
                ExitCode.ERROR.exit();
            }
        }
    }
}