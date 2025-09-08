package org.altlinux.xgradle.api.installers;

public interface PomInstaller {
    void install(String name, String searchingDir, String targetDir);
}
