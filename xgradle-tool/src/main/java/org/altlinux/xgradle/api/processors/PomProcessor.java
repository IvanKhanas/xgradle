package org.altlinux.xgradle.api.processors;

import java.nio.file.Path;
import java.util.HashMap;

public interface PomProcessor {
    void processPoms(HashMap<String,Path> pomMap);
}
