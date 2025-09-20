package org.altlinux.xgradle.api.install;

import org.altlinux.xgradle.ProcessingType;

import java.util.Optional;

public interface ArtifactsInstaller {
    void install(String searchingDirectory,
                 Optional<String> artifactName,
                 String pomInstallationDirectory,
                 String jarInstallationDirectory,
                 ProcessingType processingType);
}
