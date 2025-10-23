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

import org.altlinux.xgradle.ExitCode;
import org.altlinux.xgradle.cli.CliArgumentsContainer;
import org.altlinux.xgradle.cli.commands.CliVersion;

import java.io.FileNotFoundException;

/**
 * Default implementation of InfoController for handling application information.
 * Manages help and version information display.
 */
public class DefaultInfoController {

    /**
     * Configures and displays application information based on command-line arguments.
     * Handles help and version information display.
     *
     * @param jCommander the command-line parser
     * @param args command-line arguments
     * @param arguments parsed command-line arguments container
     * @throws FileNotFoundException if application.properties file is not found
     */
    public void configureInfo(JCommander jCommander,
                               String[] args,
                               CliArgumentsContainer arguments) throws FileNotFoundException {
        if(args.length == 0 || arguments.hasHelp()) {
            jCommander.usage();
            ExitCode.SUCCESS.exit();
        }

        if(arguments.hasVersion()) {
            CliVersion cliVersion = new CliVersion();
            cliVersion.printVersion();
            ExitCode.SUCCESS.exit();
        }
    }

}
