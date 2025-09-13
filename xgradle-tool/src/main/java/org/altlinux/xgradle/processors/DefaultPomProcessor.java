package org.altlinux.xgradle.processors;

import com.google.inject.Inject;
import org.altlinux.xgradle.api.parsers.PomParser;
import org.altlinux.xgradle.api.processors.PomProcessor;
import org.altlinux.xgradle.api.containers.PomContainer;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DefaultPomProcessor implements PomProcessor {
    private final PomParser pomParser;

    @Inject
    public DefaultPomProcessor(PomParser pomParser) {
        this.pomParser = pomParser;
    }

    @Override
    public HashMap<String, Path> processPoms(String searchingDir, Optional<String> artifactName) {
        if (artifactName.isPresent()) {
            return pomParser.getArtifactCoords(searchingDir, artifactName);
        } else {
            return pomParser.getArtifactCoords(searchingDir, Optional.empty());
        }
    }
}
