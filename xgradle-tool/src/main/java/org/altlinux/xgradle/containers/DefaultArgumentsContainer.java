package org.altlinux.xgradle.containers;

import com.beust.jcommander.Parameter;

import java.util.ArrayList;


public class DefaultArgumentsContainer {
    @Parameter
    ArrayList<String> arguments = new ArrayList<>();

    @Parameter(names = "install", description = "Install artifacts")
    private boolean install;
}
