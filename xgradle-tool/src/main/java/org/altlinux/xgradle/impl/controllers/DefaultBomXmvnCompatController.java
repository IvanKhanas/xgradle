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
import org.altlinux.xgradle.interfaces.registrars.Registrar;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Bom;
import org.altlinux.xgradle.impl.cli.CliArgumentsContainer;

import org.slf4j.Logger;

import static org.altlinux.xgradle.impl.cli.CliPreconditions.require;

/**
 * Controller for managing XMvn compatibility functions for BOM artifacts.
 * Implements {@link XmvnCompatController}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultBomXmvnCompatController implements XmvnCompatController {

    private final Registrar registrar;

    @Inject
    DefaultBomXmvnCompatController(@Bom Registrar registrar) {
        this.registrar = registrar;
    }

    @Override
    public void configureXmvnCompatFunctions(
            JCommander jCommander,
            String[] args,
            CliArgumentsContainer arguments,
            Logger logger
    ) {
        if (!arguments.hasXmvnRegister() || !arguments.hasBomRegistration()) {
            return;
        }

        require(arguments.hasSearchingDirectory(), "No searching directory specified");

        registrar.registerArtifacts(
                arguments.getSearchingDirectory(),
                arguments.getXmvnRegister(),
                arguments.getArtifactName()
        );
    }
}
