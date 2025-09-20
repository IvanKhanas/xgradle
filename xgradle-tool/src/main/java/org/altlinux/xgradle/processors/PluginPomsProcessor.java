package org.altlinux.xgradle.processors;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.altlinux.xgradle.api.parsers.PomParser;
import org.altlinux.xgradle.api.processors.PomProcessor;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;

public class PluginPomsProcessor implements PomProcessor {
    private final PomParser pluginsParser;

    @Inject
    public PluginPomsProcessor(@Named("gradlePlugins") PomParser pluginsParser) {
        this.pluginsParser = pluginsParser;
    }
    @Override
    public PluginPomsProcessor process() {
        return this;
    }

    @Override
    public HashMap<String, Path> pomsFromDirectory(String searchingDir, Optional<String> artifactName) {
        return pluginsParser.parsePoms().getArtifactCoords(searchingDir, artifactName);
    }

}
