package org.altlinux.xgradle.api.controllers;

import com.beust.jcommander.JCommander;
import org.altlinux.xgradle.ProcessingType;
import org.altlinux.xgradle.cli.CliArgumentsContainer;
import org.slf4j.Logger;

import java.util.Optional;

public interface ArtifactsInstallationController extends Controller {

    @Override
    ArtifactsInstallationController configure();

    void configurePluginArtifactsInstallation(JCommander jCommander,
                                              String[] args,
                                              CliArgumentsContainer arguments,
                                              Logger logger);
}
