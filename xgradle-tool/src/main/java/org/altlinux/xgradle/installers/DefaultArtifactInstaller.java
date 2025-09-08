package org.altlinux.xgradle.installers;

import org.altlinux.xgradle.api.installers.ArtifactInstaller;
import org.altlinux.xgradle.containers.DefaultArtifactContainer;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class DefaultArtifactInstaller implements ArtifactInstaller {
    private static final Logger logger = LogManager.getLogger(DefaultArtifactInstaller.class);
    private final DefaultArtifactContainer artifactContainer;

    public DefaultArtifactInstaller() {
        this.artifactContainer = new DefaultArtifactContainer();
    }

    @Override
    public void install(String searchingDir, String targetDir) throws IOException {
        Path target = Path.of(targetDir);

        if (!Files.exists(target)) {
            Files.createDirectories(target);
            logger.info("Created directory:" + target);
        }

        for(Map.Entry<String, Path> entry : artifactContainer.getArtifacts(searchingDir).entrySet()) {
            String artifactName = entry.getKey();
            Path sourceArtifact = entry.getValue();
            Path targetArtifact = target.resolve(artifactName);

            Files.copy(sourceArtifact, targetArtifact, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void installSelected(String searchingDir, String targetDir, String name) throws IOException {
        Path target = Path.of(targetDir);

        if (!Files.exists(target)) {
            Files.createDirectories(target);
            logger.info("Created directory: " + target);
        }

            Path sourceArtifact = artifactContainer.getArtifacts(searchingDir).get(name);
            Path targetArtifact = target.resolve(name);

            Files.copy(sourceArtifact, targetArtifact, StandardCopyOption.REPLACE_EXISTING);
    }
}
