package org.altlinux.xgradle.containers;

import com.google.inject.Inject;
import org.altlinux.xgradle.api.collectors.PomCollector;
import org.altlinux.xgradle.api.containers.PomContainer;
import org.altlinux.xgradle.collectors.DefaultPomCollector;

import java.nio.file.Path;
import java.util.HashMap;

public class DefaultPomContainer implements PomContainer {
    private final PomCollector pomCollector;

    @Inject
    public DefaultPomContainer(PomCollector pomCollector) {
        this.pomCollector = pomCollector;
    }

    @Override
    public HashMap<String,Path> getPoms(String searchingDir) {
        return pomCollector.collect(searchingDir);
    }
}
