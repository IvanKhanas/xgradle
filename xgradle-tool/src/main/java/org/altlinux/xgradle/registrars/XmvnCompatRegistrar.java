package org.altlinux.xgradle.registrars;

import com.google.inject.Inject;

import org.altlinux.xgradle.api.cli.CommandExecutor;
import org.altlinux.xgradle.api.cli.CommandLineParser;
import org.altlinux.xgradle.api.containers.ArtifactContainer;
import org.altlinux.xgradle.api.registrars.Registrar;

import java.io.IOException;
import java.util.Optional;

public class XmvnCompatRegistrar implements Registrar {
    private final ArtifactContainer artifactContainer;
    private final CommandExecutor commandExecutor;
    private final CommandLineParser commandLineParser;

    @Inject
    public XmvnCompatRegistrar(ArtifactContainer artifactContainer,
                               CommandExecutor commandExecutor,
                               CommandLineParser commandLineParser) {
        this.artifactContainer = artifactContainer;
        this.commandExecutor = commandExecutor;
        this.commandLineParser = commandLineParser;
    }

    @Override
    public void registerArtifacts(String searchingDir, String xmvnRegisterCommanString , String registerCommand, Optional<String> artifactName) {
    }

}
