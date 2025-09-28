package org.altlinux.xgradle.processors;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.altlinux.xgradle.api.parsers.PomParser;
import org.altlinux.xgradle.api.processors.PomProcessor;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

public class DefaultBomProcessor implements PomProcessor<Set<Path>> {
    private final PomParser<Set<Path>> pomParser;

    @Inject
    public DefaultBomProcessor(@Named("Bom")PomParser<Set<Path>> pomParser) {
        this.pomParser = pomParser;
    }

    @Override
    public DefaultBomProcessor process() {
        return this;
    }

    @Override
    public Set<Path> pomsFromDirectory(String searchingDir, Optional<String> artifactName) {
        if (artifactName.isPresent()) {
            return pomParser.parsePoms().getArtifactCoords(searchingDir, artifactName);
        } else {
            return pomParser.parsePoms().getArtifactCoords(searchingDir, Optional.empty());
        }
    }
}
