package org.altlinux.xgradle.cli;

import org.altlinux.xgradle.api.cli.CommandExecutor;

import java.io.IOException;

public class DefaultCommandExecutor implements CommandExecutor {

    @Override
    public int execute(ProcessBuilder processBuilder) throws IOException, InterruptedException {
        Process process = processBuilder.start();
        return process.waitFor();
    }
}