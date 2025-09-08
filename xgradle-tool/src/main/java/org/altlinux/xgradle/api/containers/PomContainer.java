package org.altlinux.xgradle.api.containers;

import java.nio.file.Path;
import java.util.HashMap;

public interface PomContainer {

    HashMap<String,Path> getPoms(String searchingDirectory);
}
