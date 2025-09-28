package org.altlinux.xgradle.registrars;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.altlinux.xgradle.api.cli.CommandExecutor;
import org.altlinux.xgradle.api.cli.CommandLineParser;
import org.altlinux.xgradle.api.processors.PomProcessor;
import org.altlinux.xgradle.api.registrars.Registrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.List;
import java.util.Set;

public class DefaultXmvnBomCompatRegistrar implements Registrar {
    private static final Logger logger = LoggerFactory.getLogger("XgradleLogger");
    private final PomProcessor<Set<Path>> pomProcessor;
    private final CommandExecutor commandExecutor;
    private final CommandLineParser commandLineParser;

    @Inject
    public DefaultXmvnBomCompatRegistrar(
            @Named("Bom")PomProcessor<Set<Path>> pomProcessor,
            CommandExecutor commandExecutor,
            CommandLineParser commandLineParser
    ) {
        this.pomProcessor = pomProcessor;
        this.commandExecutor = commandExecutor;
        this.commandLineParser = commandLineParser;
    }

    @Override
    public void registerArtifacts(String searchingDir, String command, Optional<String> artifactName) {
        Set<Path> artifacts;

        if (artifactName.isPresent()) {
            artifacts = pomProcessor.pomsFromDirectory(searchingDir, artifactName);
        }else {
            artifacts = pomProcessor.pomsFromDirectory(searchingDir, Optional.empty());
        }

        List<String> commandParts = commandLineParser.parseCommandLine(command);

        for (Path part : artifacts) {
            List<String> currentCommand = new ArrayList<>(commandParts);
            currentCommand.add(part.toString());
            logger.info("Running: " + String.join(" ", currentCommand));

            ProcessBuilder processBuilder = new ProcessBuilder(currentCommand);
            processBuilder.redirectErrorStream(true);

            try {
                int exitCode = commandExecutor.execute(processBuilder);

                if (exitCode != 0) {
                    throw new RuntimeException("Failed to register artifact, exit code: " + exitCode);
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (artifacts.isEmpty()) {
            logger.info("No BOM registered");
        }else {
            logger.info("BOM`s registered successfully");
        }
    }


}
