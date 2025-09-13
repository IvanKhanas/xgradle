package org.altlinux.xgradle.api.cli;

import java.io.IOException;

public interface CommandCreator {

    ProcessBuilder formCommand(ProcessBuilder processBuilder) throws IOException;

    ProcessBuilder formXmvnRegisterCommandForArtifact(String command, String artifact)
}
