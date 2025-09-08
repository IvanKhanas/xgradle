package org.altlinux.xgradle.containers;

import org.altlinux.xgradle.api.collectors.ArtifactCollector;
import org.altlinux.xgradle.api.containers.ArtifactContainer;
import org.altlinux.xgradle.collectors.DefaultArtifactCollector;

import java.nio.file.Path;
import java.util.HashMap;

public class DefaultArtifactContainer implements ArtifactContainer {
    private final ArtifactCollector artifactCollector;

    public DefaultArtifactContainer() {
        this.artifactCollector = new DefaultArtifactCollector();
    }

    @Override
    public HashMap<String, Path> getArtifacts(String searchingDirectory) {
        return artifactCollector.collect(searchingDirectory);
    }
}
