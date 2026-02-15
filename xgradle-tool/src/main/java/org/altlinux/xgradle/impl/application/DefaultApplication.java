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

package org.altlinux.xgradle.impl.application;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.application.Application;
import org.altlinux.xgradle.interfaces.controllers.ArtifactsInstallationController;
import org.altlinux.xgradle.interfaces.controllers.PomRedactionController;
import org.altlinux.xgradle.interfaces.controllers.XmvnCompatController;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Bom;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Javadoc;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Library;
import org.altlinux.xgradle.impl.cli.CliArgumentsContainer;
import org.altlinux.xgradle.impl.cli.commands.CliVersion;
import org.altlinux.xgradle.impl.enums.ExitCode;
import org.altlinux.xgradle.impl.exceptions.CliUsageException;

import org.slf4j.Logger;

import javax.inject.Provider;
import java.io.FileNotFoundException;

/**
 * Default implementation of {@link Application}.
 * Implements {@link Application}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultApplication implements Application {

    private final JCommander jCommander;
    private final CliArgumentsContainer cliArgs;
    private final Logger logger;

    private final Provider<XmvnCompatController> libraryXmvnController;
    private final Provider<XmvnCompatController> bomXmvnController;
    private final Provider<XmvnCompatController> javadocXmvnController;
    private final Provider<ArtifactsInstallationController> pluginsController;
    private final Provider<PomRedactionController> pomRedactionController;

    @Inject
    DefaultApplication(
            JCommander jCommander,
            CliArgumentsContainer cliArgs,
            Logger logger,
            @Library Provider<XmvnCompatController> libraryXmvnController,
            @Bom Provider<XmvnCompatController> bomXmvnController,
            @Javadoc Provider<XmvnCompatController> javadocXmvnController,
            Provider<ArtifactsInstallationController> pluginsController,
            Provider<PomRedactionController> pomRedactionController
    ) {
        this.jCommander = jCommander;
        this.cliArgs = cliArgs;
        this.logger = logger;
        this.libraryXmvnController = libraryXmvnController;
        this.bomXmvnController = bomXmvnController;
        this.javadocXmvnController = javadocXmvnController;
        this.pluginsController = pluginsController;
        this.pomRedactionController = pomRedactionController;
    }

    @Override
    public ExitCode run(String[] args) {
        try {
            jCommander.parse(args);
            cliArgs.validateMutuallyExclusive();
        } catch (ParameterException e) {
            logger.error(e.getMessage());
            jCommander.usage();
            return ExitCode.ERROR;
        } catch (RuntimeException e) {
            logger.error(e.getMessage(), e);
            return ExitCode.ERROR;
        }

        if (args.length == 0 || cliArgs.hasHelp()) {
            jCommander.usage();
            return ExitCode.SUCCESS;
        }

        if (cliArgs.hasPomRedaction()) {
            try {
                pomRedactionController.get().configure();
                return ExitCode.SUCCESS;
            } catch (RuntimeException e) {
                logger.error(e.getMessage(), e);
                return ExitCode.ERROR;
            }
        }

        if (cliArgs.hasVersion()) {
            try {
                new CliVersion().printVersion();
                return ExitCode.SUCCESS;
            } catch (FileNotFoundException e) {
                logger.error("Could not find application.properties", e);
                return ExitCode.ERROR;
            }
        }

        try {
            libraryXmvnController.get().configureXmvnCompatFunctions(jCommander, args, cliArgs, logger);
            pluginsController.get().configurePluginArtifactsInstallation(jCommander, args, cliArgs, logger);
            bomXmvnController.get().configureXmvnCompatFunctions(jCommander, args, cliArgs, logger);
            javadocXmvnController.get().configureXmvnCompatFunctions(jCommander, args, cliArgs, logger);
            return ExitCode.SUCCESS;
        } catch (CliUsageException e) {
            logger.error(e.getMessage());
            jCommander.usage();
            return ExitCode.ERROR;
        } catch (RuntimeException e) {
            logger.error(e.getMessage(), e);
            return ExitCode.ERROR;
        }
    }
}
