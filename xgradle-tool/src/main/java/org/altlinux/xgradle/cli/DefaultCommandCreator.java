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
package org.altlinux.xgradle.cli;

import com.google.inject.Inject;

import org.altlinux.xgradle.api.cli.CommandCreator;
import org.altlinux.xgradle.api.cli.CommandLineParser;

import java.util.ArrayList;
import java.util.List;

public class DefaultCommandCreator implements CommandCreator {
    private final CommandLineParser commandLineParser;

    @Inject
    public DefaultCommandCreator(CommandLineParser commandLineParser) {
        this.commandLineParser = commandLineParser;
    }

    @Override
    public ProcessBuilder formCommand(ProcessBuilder processBuilder) {
        return processBuilder;
    }

    public ProcessBuilder formXmvnRegisterCommandForArtifact(String command, String artifact) {
        commandLineParser.parseCommandLine(command);
        List<String> commandParts = new ArrayList<>();

        commandParts.add(artifact);

        ProcessBuilder processBuilder = new ProcessBuilder(commandParts);
        processBuilder.redirectErrorStream(true);

        return processBuilder;
    }
}
