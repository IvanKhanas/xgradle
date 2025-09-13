package org.altlinux.xgradle.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.util.ArrayList;
import java.util.List;

@Parameters(separators = "=")
public class DefaultArgumentsContainer {

    @Parameter
    List<String> arguments = new ArrayList<>();

    @Parameter(names = "--xmvn-register", description = "Command to register an artifact")
    private String xmvnRegister;

    @Parameter(description = "An artifact to process")
    private List<String> artifacts = new ArrayList<>();
}