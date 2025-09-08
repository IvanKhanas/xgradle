package org.altlinux.xgradle.processors;

import com.google.inject.Inject;
import org.altlinux.xgradle.api.processors.PomProcessor;
import org.altlinux.xgradle.api.containers.PomContainer;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class DefaultPomProcessor implements PomProcessor {
    //private final Map<String,Path> jarNames;
    private final PomContainer pomContainer;

    @Inject
    public DefaultPomProcessor(PomContainer pomContainer) {
        this.pomContainer = pomContainer;
    }

    @Override
    public void processPoms(HashMap<String,Path> pomMap) {

    }
}
