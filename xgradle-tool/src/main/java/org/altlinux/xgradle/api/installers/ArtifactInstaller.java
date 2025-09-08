package org.altlinux.xgradle.api.installers;

import java.io.IOException;

public interface ArtifactInstaller {
    void install(String searchingDir, String targetDir) throws IOException;
}
