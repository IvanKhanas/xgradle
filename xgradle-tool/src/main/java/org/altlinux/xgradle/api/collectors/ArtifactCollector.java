package org.altlinux.xgradle.api.collectors;

import java.nio.file.Path;
import java.util.HashMap;

public interface ArtifactCollector {

    HashMap<String, Path> collect(String searchingDirectory);
}
