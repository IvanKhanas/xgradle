package org.altlinux.xgradle.api.cli;

import java.util.List;

public interface CommandLineParser {

    List<String> parseCommandLine(String command);
}
