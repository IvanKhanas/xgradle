package org.altlinux.xgradle.api.collectors;

import java.nio.file.Path;
import java.util.HashMap;

public interface PomCollector {
    HashMap<String, Path> collect(String searchingDir);
}
