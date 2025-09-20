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
package org.altlinux.xgradle.registrars;

import com.google.inject.Inject;

import org.altlinux.xgradle.ProcessingType;
import org.altlinux.xgradle.api.cli.CommandExecutor;
import org.altlinux.xgradle.api.cli.CommandLineParser;
import org.altlinux.xgradle.api.containers.ArtifactContainer;
import org.altlinux.xgradle.api.registrars.Registrar;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.List;

public class XmvnCompatRegistrar implements Registrar {
    private final ArtifactContainer artifactContainer;
    private final CommandExecutor commandExecutor;
    private final CommandLineParser commandLineParser;

    @Inject
    public XmvnCompatRegistrar(ArtifactContainer artifactContainer,
                               CommandExecutor commandExecutor,
                               CommandLineParser commandLineParser) {
        this.artifactContainer = artifactContainer;
        this.commandExecutor = commandExecutor;
        this.commandLineParser = commandLineParser;
    }

    @Override
    public void registerArtifacts(String searchingDir, String registerCommand, Optional<String> artifactName) {
        Map<String, Path> artifacts;
        if (artifactName.isPresent()) {
            artifacts = artifactContainer.getArtifacts(searchingDir, artifactName, ProcessingType.LIBRARY);
        }else {
            artifacts = artifactContainer.getArtifacts(searchingDir, Optional.empty(), ProcessingType.LIBRARY);
        }

        List<String> commandParts = commandLineParser.parseCommandLine(registerCommand);

        for(Map.Entry<String, Path> entry : artifacts.entrySet()) {
            String pomPath = entry.getKey();
            Path jarPath = entry.getValue();

            commandParts.add(pomPath);
            commandParts.add(jarPath.toString());
            System.out.println("\nRunning: " + String.join(" ", commandParts));

            ProcessBuilder processBuilder = new ProcessBuilder(commandParts);
            processBuilder.redirectErrorStream(true);


            try {
                int exitCode = commandExecutor.execute(processBuilder);
                if (exitCode != 0) {
                    throw new RuntimeException("Failed to register artifact, exit code: " + exitCode);
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Failed to execute command", e);
            }
            commandParts.remove(pomPath);
            commandParts.remove(jarPath.toString());
        }
    }
}
