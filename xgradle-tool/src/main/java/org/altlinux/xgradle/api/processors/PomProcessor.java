package org.altlinux.xgradle.api.processors;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;

public interface PomProcessor {
    HashMap<String, Path> processPoms(String searchingDir, Optional<String> artifactName);
}
