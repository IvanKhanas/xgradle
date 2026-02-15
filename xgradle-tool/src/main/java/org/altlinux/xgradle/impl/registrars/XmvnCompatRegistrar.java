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
package org.altlinux.xgradle.impl.registrars;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.impl.enums.ExitCode;
import org.altlinux.xgradle.impl.enums.ProcessingType;
import org.altlinux.xgradle.interfaces.cli.CommandExecutor;
import org.altlinux.xgradle.interfaces.cli.CommandLineParser;
import org.altlinux.xgradle.interfaces.containers.ArtifactContainer;
import org.altlinux.xgradle.interfaces.registrars.Registrar;

import org.altlinux.xgradle.impl.exceptions.CommandExecutionException;
import org.altlinux.xgradle.impl.exceptions.EmptyRegisterCommandException;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.List;

/**
 * Registrar implementation for XMvn compatibility with library artifacts.
 * Implements {@link Registrar}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class XmvnCompatRegistrar implements Registrar {

    private final ArtifactContainer artifactContainer;
    private final CommandExecutor commandExecutor;
    private final CommandLineParser commandLineParser;
    private final Logger logger;

    @Inject
    XmvnCompatRegistrar(
            ArtifactContainer artifactContainer,
            CommandExecutor commandExecutor,
            CommandLineParser commandLineParser,
            Logger logger
    ) {
        this.artifactContainer = artifactContainer;
        this.commandExecutor = commandExecutor;
        this.commandLineParser = commandLineParser;
        this.logger = logger;
    }

    @Override
    public void registerArtifacts(String searchingDir, String registerCommand, Optional<List<String>> artifactName) {
        Map<String, Path> artifacts;
        if (artifactName.isPresent()) {
            artifacts = artifactContainer.getArtifacts(searchingDir, artifactName, ProcessingType.LIBRARY);
        } else {
            artifacts = artifactContainer.getArtifacts(searchingDir, Optional.empty(), ProcessingType.LIBRARY);
        }

        List<String> baseCommand = commandLineParser.parseCommandLine(registerCommand);

        if (baseCommand == null || baseCommand.isEmpty()) {
            throw new EmptyRegisterCommandException(registerCommand);
        }

        for (Map.Entry<String, Path> entry : artifacts.entrySet()) {
            String pomPath = entry.getKey();
            Path jarPath = entry.getValue();

            List<String> currentCommand = new ArrayList<>(baseCommand);
            currentCommand.add(pomPath);
            currentCommand.add(jarPath.toString());

            logger.info("\nRegistering pair: " + String.join(" ", currentCommand));

            ProcessBuilder processBuilder = new ProcessBuilder(currentCommand);

            try {
                int exitCode = commandExecutor.execute(processBuilder);
                if (exitCode != ExitCode.SUCCESS.getExitCode()) {
                    throw new RuntimeException("Failed to register artifact, exit code: " + exitCode);
                }
            } catch (IOException | InterruptedException e) {
                throw new CommandExecutionException(currentCommand, e);
            }
        }

        if (artifacts.isEmpty()) {
            logger.info("No artifacts registered");
        } else {
            logger.info("Artifacts registered successfully");
        }
    }
}
