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

import org.altlinux.xgradle.interfaces.controllers.XmvnCompatController;
import org.altlinux.xgradle.interfaces.installers.JavadocInstaller;
import org.altlinux.xgradle.impl.cli.CliArgumentsContainer;

import org.slf4j.Logger;

import static org.altlinux.xgradle.impl.cli.CliPreconditions.require;

/**
 * Controller for managing Javadoc installation.
 * Implements {@link XmvnCompatController}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultJavadocXmvnCompatController implements XmvnCompatController {

    private final JavadocInstaller javadocInstaller;

    @Inject
    DefaultJavadocXmvnCompatController(JavadocInstaller javadocInstaller) {
        this.javadocInstaller = javadocInstaller;
    }

    @Override
    public void configureXmvnCompatFunctions(
            JCommander jCommander,
            String[] args,
            CliArgumentsContainer arguments,
            Logger logger
    ) {
        if (!arguments.hasJavadocRegistration()) {
            return;
        }

        require(arguments.hasSearchingDirectory(), "No searching directory specified for Javadoc installation");
        require(arguments.hasJarInstallationDirectory(), "No JAR installation directory specified for Javadoc installation");

        javadocInstaller.installJavadoc(
                arguments.getSearchingDirectory(),
                arguments.getArtifactName(),
                arguments.getJarInstallationDirectory()
        );
    }
}
