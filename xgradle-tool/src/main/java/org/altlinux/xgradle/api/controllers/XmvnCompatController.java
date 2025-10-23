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
package org.altlinux.xgradle.api.controllers;

import com.beust.jcommander.JCommander;

import org.altlinux.xgradle.cli.CliArgumentsContainer;

import org.slf4j.Logger;

/**
 * Interface for XMvn compatibility controller operations.
 * Defines contract for configuring and managing XMvn compatibility functions.
 *
 * @author Ivan Khanas
 */
public interface XmvnCompatController {

    /**
     * Configures and executes XMvn compatibility functions based on command-line arguments.
     *
     * @param jCommander the command-line parser
     * @param args command-line arguments
     * @param cliArgumentsContainer parsed command-line arguments container
     * @param logger logger for error and information messages
     */
    void configureXmvnCompatFunctions(JCommander jCommander,
                                      String[] args,
                                      CliArgumentsContainer cliArgumentsContainer,
                                      Logger logger);
}