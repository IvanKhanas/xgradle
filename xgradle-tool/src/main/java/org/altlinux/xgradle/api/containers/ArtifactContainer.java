package org.altlinux.xgradle.api.containers;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

public interface ArtifactContainer {
    HashMap<String, Path> getArtifacts(String searchingDir, Optional<String> artifactName);

    Collection<Path> getArtifactPaths(String searchingDir, Optional<String> artifactName);

    Collection<String> getArtifactSignatures(String searchingDir, Optional<String> artifactName);
}
