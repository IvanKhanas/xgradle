package org.altlinux.xgradle.containers;

import com.google.inject.Inject;
import org.altlinux.xgradle.api.collectors.PomCollector;
import org.altlinux.xgradle.api.containers.PomContainer;
import org.altlinux.xgradle.collectors.DefaultPomCollector;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;

public class DefaultPomContainer implements PomContainer {
    private final PomCollector pomCollector;

    @Inject
    public DefaultPomContainer(PomCollector pomCollector) {
        this.pomCollector = pomCollector;
    }

    @Override
    public HashMap<String,Path> getAllPoms(String searchingDir) {
        return pomCollector.collectAll(searchingDir);
    }

    @Override
    public Collection<Path> getAllPomPaths(String searchingDir) {
        return pomCollector.collectAll(searchingDir).values();
    }

    @Override
    public Collection<String> getAllPomSignatures(String searchingDir) {
        return pomCollector.collectAll(searchingDir).keySet();
    }

    @Override
    public HashMap<String, Path> getSelectedPoms(String searchingDir, String artifactName) {
        return pomCollector.collectSelected(searchingDir, artifactName);
    }

    @Override
    public Collection<Path> getSelectedPomPaths(String searchingDir, String artifactName) {
        return pomCollector.collectSelected(searchingDir, artifactName).values();
    }
}
