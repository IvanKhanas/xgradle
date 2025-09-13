package org.altlinux.xgradle.collectors;

import com.google.inject.Inject;
import org.altlinux.xgradle.api.collectors.ArtifactCollector;
import org.altlinux.xgradle.api.processors.PomProcessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class DefaultArtifactCollector implements ArtifactCollector {
    private final PomProcessor pomProcessor;

    @Inject
    public DefaultArtifactCollector(PomProcessor pomProcessor) {
        this.pomProcessor = pomProcessor;
    }

    @Override
    public HashMap<String,Path> collect(String searchingDir, Optional<String> artifactName) {
        if (artifactName.isPresent()) {
            return pomProcessor.processPoms(searchingDir, artifactName);
        }
        return pomProcessor.processPoms(searchingDir, Optional.empty());
    }
}
