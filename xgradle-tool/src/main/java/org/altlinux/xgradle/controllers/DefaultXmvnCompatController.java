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

import org.altlinux.xgradle.api.controllers.XmvnCompatController;
import org.altlinux.xgradle.api.registrars.Registrar;
import org.altlinux.xgradle.cli.CliArgumentsContainer;

import org.slf4j.Logger;

import java.util.Arrays;

public class DefaultXmvnCompatController implements XmvnCompatController {
    private final Registrar registrar;

    @Inject
    public DefaultXmvnCompatController(Registrar registrar) {
        this.registrar = registrar;
    }

    @Override
    public DefaultXmvnCompatController configure() {
        return this;
    }

    @Override
    public void configureXmvnCompatFunctions(JCommander jCommander, String[] args, CliArgumentsContainer arguments, Logger logger) {

        if(arguments.hasHelp() || args.length < 1) {
            jCommander.usage();
        }

            if(arguments.hasXmvnRegister()) {
                if(arguments.hasSearchingDirectory()) {
                    try {
                        registrar.registerArtifacts(
                                arguments.getSearchingDirectory(),
                                arguments.getXmvnRegister(),
                                arguments.getArtifactName()
                        );
                        logger.info("Artifacts registered successfully");
                    }catch (Exception e) {
                        logger.error("Error: {}", e.getMessage());
                        System.exit(1);
                    }
                } else {
                    logger.error("No searching directory specified");
                    logger.error("Usage: \n xgradle-tool --xmvn-register=\"<registration command>\" --searching-directory=\"<directory path>\" | (optional) --artifact=<artifactName>\"");
                    System.exit(1);
                }
            }
    }
}
