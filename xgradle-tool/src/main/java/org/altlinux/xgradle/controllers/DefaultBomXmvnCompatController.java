package org.altlinux.xgradle.controllers;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.beust.jcommander.JCommander;
import org.altlinux.xgradle.api.controllers.XmvnCompatController;
import org.altlinux.xgradle.api.registrars.Registrar;
import org.altlinux.xgradle.cli.CliArgumentsContainer;
import org.slf4j.Logger;

public class DefaultBomXmvnCompatController implements XmvnCompatController {
    private final Registrar registrar;

    @Inject
    public DefaultBomXmvnCompatController(@Named("Bom")Registrar registrar) {
        this.registrar = registrar;
    }

    @Override
    public DefaultBomXmvnCompatController configure() {
        return this;
    }

    @Override
    public void configureXmvnCompatFunctions(JCommander jCommander, String[] args, CliArgumentsContainer arguments, Logger logger) {
        if (arguments.hasXmvnRegister()) {
            if (arguments.hasBomInstallation()) {
                if (arguments.hasSearchingDirectory()) {
                    registrar.registerArtifacts(
                            arguments.getSearchingDirectory(),
                            arguments.getXmvnRegister(),
                            arguments.getArtifactName()
                    );
                }
            }
        }
    }
}
