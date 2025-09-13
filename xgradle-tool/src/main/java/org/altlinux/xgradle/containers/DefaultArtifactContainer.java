package org.altlinux.xgradle.containers;

import com.google.inject.Inject;
import org.altlinux.xgradle.api.collectors.ArtifactCollector;
import org.altlinux.xgradle.api.containers.ArtifactContainer;
import org.altlinux.xgradle.collectors.DefaultArtifactCollector;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

public class DefaultArtifactContainer implements ArtifactContainer {
    private final ArtifactCollector artifactCollector;

    @Inject
    public DefaultArtifactContainer(ArtifactCollector artifactCollector) {
        this.artifactCollector = artifactCollector;
    }

    @Override
    public HashMap<String, Path> getArtifacts(String searchingDirectory, Optional<String> artifactName) {
        if (artifactName.isPresent()) {
            return artifactCollector.collect(searchingDirectory, artifactName);
        } else {
            return artifactCollector.collect(searchingDirectory, Optional.empty());
        }
    }

    @Override
    public Collection<Path> getArtifactPaths(String searchingDirectory, Optional<String> artifactName) {
        if (artifactName.isPresent()) {
            return artifactCollector.collect(searchingDirectory,artifactName).values();
        } else {
            return artifactCollector.collect(searchingDirectory, Optional.empty()).values();
        }
    }

    @Override
    public Collection<String> getArtifactSignatures(String searchingDirectory, Optional<String> artifactName) {
        if (artifactName.isPresent()) {
            return artifactCollector.collect(searchingDirectory, artifactName).keySet();
        } else {
            return artifactCollector.collect(searchingDirectory, Optional.empty()).keySet();
        }
    }
}
