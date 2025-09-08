package org.altlinux.xgradle.api.containers;

import java.nio.file.Path;
import java.util.HashMap;

public interface ArtifactContainer {
    HashMap<String, Path> getArtifacts(String searchingDirectory);
}
