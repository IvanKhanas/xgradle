package org.altlinux.xgradle.installers;

import com.google.inject.Inject;
import org.altlinux.xgradle.api.containers.PomContainer;
import org.altlinux.xgradle.api.installers.PomInstaller;
import org.altlinux.xgradle.containers.DefaultPomContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class DefaultPomInstaller implements PomInstaller {
    private static final Logger logger = LogManager.getLogger(DefaultArtifactInstaller.class);
    private final PomContainer pomContainer;

    @Inject
    public DefaultPomInstaller(PomContainer pomContainer) {
        this.pomContainer = pomContainer;
    }

    @Override
    public void install(String name, String searchingDir, String targetDir) {
        Path target = Path.of(targetDir);

        if(!Files.exists(target)) {
            try {
                Files.createDirectories(target);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create directory" + target,e);
            }
            logger.info("Created directory: " + target);
        }

        Path sourcePom = pomContainer.getPoms(searchingDir).get(name);
        Path targetPom = target.resolve(name);

        try {
            Files.copy(sourcePom, targetPom, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy files",e);
        }
    }
}
