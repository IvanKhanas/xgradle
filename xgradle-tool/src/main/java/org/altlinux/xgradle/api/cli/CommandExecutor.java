package org.altlinux.xgradle.api.cli;

import java.io.IOException;

public interface CommandExecutor {

    int execute(ProcessBuilder processBuilder) throws IOException, InterruptedException;


}
