package org.altlinux.xgradle.cli;

import com.google.inject.Inject;

import org.altlinux.xgradle.api.cli.CommandCreator;
import org.altlinux.xgradle.api.cli.CommandLineParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DefaultCommandCreator implements CommandCreator {
    private final CommandLineParser commandLineParser;

    @Inject
    public DefaultCommandCreator(CommandLineParser commandLineParser) {
        this.commandLineParser = commandLineParser;
    }

    @Override
    public ProcessBuilder formCommand(ProcessBuilder processBuilder) throws IOException {
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
