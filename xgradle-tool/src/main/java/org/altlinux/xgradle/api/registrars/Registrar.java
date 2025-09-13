package org.altlinux.xgradle.api.registrars;

import java.util.Optional;

public interface Registrar {

    void registerArtifacts(String searchingDir, String registerCommand, Optional<String> artifactName);
}
