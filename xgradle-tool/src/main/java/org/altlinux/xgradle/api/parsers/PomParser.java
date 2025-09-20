package org.altlinux.xgradle.api.parsers;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;

public interface PomParser {
    PomParser parsePoms();

    HashMap<String, Path> getArtifactCoords(String searchingDir, Optional<String> artifactName);

}
