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
import com.google.inject.name.Named;

import org.altlinux.xgradle.ExitCode;
import org.altlinux.xgradle.api.controllers.XmvnCompatController;
import org.altlinux.xgradle.api.registrars.Registrar;
import org.altlinux.xgradle.cli.CliArgumentsContainer;

import org.slf4j.Logger;

/**
 * Controller for managing XMvn compatibility functions for library artifacts.
 * Handles command-line configuration and execution of XMvn registration for libraries.
 *
 * @author Ivan Khanas
 */
@Singleton
public class DefaultXmvnCompatController implements XmvnCompatController {
    private final Registrar registrar;

    /**
     * Constructs a new DefaultXmvnCompatController with required dependencies.
     *
     * @param registrar registrar for library artifacts
     */
    @Inject
    public DefaultXmvnCompatController(@Named("Library")Registrar registrar) {
        this.registrar = registrar;
    }

    /**
     * Configures and executes XMvn compatibility functions for library artifacts.
     * Validates parameters and executes registration if requirements are met.
     *
     * @param jCommander the command-line parser
     * @param args command-line arguments
     * @param arguments parsed command-line arguments container
     * @param logger logger for error and information messages
     */
    @Override
    public void configureXmvnCompatFunctions(JCommander jCommander, String[] args, CliArgumentsContainer arguments, Logger logger) {

            if(arguments.hasXmvnRegister()
                    && !arguments.hasBomRegistration()
                    && !arguments.hasJavadocRegistration())
            {
                if(arguments.hasSearchingDirectory()) {
                    try {
                        registrar.registerArtifacts(
                                arguments.getSearchingDirectory(),
                                arguments.getXmvnRegister(),
                                arguments.getArtifactName()
                        );
                    }catch (Exception e) {
                        logger.error("Error: {}", e.getMessage());
                        ExitCode.ERROR.exit();
                    }
                }else {
                    logger.error("No searching directory specified");
                    ExitCode.ERROR.exit();
                }
            }
    }
}
