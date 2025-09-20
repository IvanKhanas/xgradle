package org.altlinux.xgradle.controllers;

import com.beust.jcommander.JCommander;

import com.google.inject.Inject;

import org.altlinux.xgradle.ProcessingType;
import org.altlinux.xgradle.api.controllers.ArtifactsInstallationController;
import org.altlinux.xgradle.api.install.ArtifactsInstaller;
import org.altlinux.xgradle.cli.CliArgumentsContainer;
import org.slf4j.Logger;

public class DefaultPluginsInstallationController implements ArtifactsInstallationController {
    private final ArtifactsInstaller pluginArtifactsInstaller;

    @Inject
    public DefaultPluginsInstallationController(ArtifactsInstaller pluginArtifactsInstaller) {
        this.pluginArtifactsInstaller = pluginArtifactsInstaller;
    }

    @Override
    public ArtifactsInstallationController configure() {
        return this;
    }

    @Override
    public void configurePluginArtifactsInstallation(JCommander jCommander,
                                                     String[] args,
                                                     CliArgumentsContainer arguments,
                                                     Logger logger)
    {
        if(arguments.hasInstallPluginParameter()) {
            if(arguments.hasSearchingDirectory()) {
                if(arguments.hasArtifactName()) {
                    if(arguments.hasPomInstallationDirectory()){
                        if(arguments.hasJarInstallationDirectory()) {
                            pluginArtifactsInstaller.install(
                                    arguments.getSearchingDirectory(),
                                    arguments.getArtifactName(),
                                    arguments.getPomInstallationDirectory(),
                                    arguments.getJarInstallationDirectory(),
                                    ProcessingType.PLUGINS
                            );
                        }else {
                            logger.error("No Jar installation directory specified");
                            jCommander.usage();
                            System.exit(1);
                        }
                    }else {
                        logger.error("No POM installation directory specified");
                        jCommander.usage();
                        System.exit(1);
                    }
                }else {
                    logger.error("Please specify an artifact name. (--artifact=<artifactName>)");
                    jCommander.usage();
                    System.exit(1);
                }
            }else {
                logger.error("No searching directory specified");
                jCommander.usage();
                System.exit(1);
            }
        }
    }
}
