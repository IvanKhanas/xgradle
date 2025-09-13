package org.altlinux.xgradle.api.containers;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;

public interface PomContainer {

    HashMap<String,Path> getAllPoms(String searchingDir);

    Collection<Path> getAllPomPaths(String searchingDir);

    Collection<String> getAllPomSignatures(String searchingDir);

    HashMap<String, Path> getSelectedPoms(String searchingDir, String artifactName);

    Collection<Path> getSelectedPomPaths(String searchingDir, String artifactName);
}
