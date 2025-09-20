package org.altlinux.xgradle.installers;

import com.google.inject.Inject;

import org.altlinux.xgradle.ProcessingType;
import org.altlinux.xgradle.api.containers.ArtifactContainer;
import org.altlinux.xgradle.api.install.ArtifactsInstaller;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class DefaultPluginArtifactsInstaller implements ArtifactsInstaller {
    private final Logger logger = LoggerFactory.getLogger("XgradleLogger");
    private final ArtifactContainer artifactConstainer;

    @Inject
    public DefaultPluginArtifactsInstaller(ArtifactContainer artifactConstainer) {
        this.artifactConstainer = artifactConstainer;
    }

    @Override
    public void install(String searchingDirectory,
                        Optional<String> artifactName,
                        String pomInstallationDirectory,
                        String jarInstallationDirectory,
                        ProcessingType processingType) {
        HashMap<String, Path> artifactsMap = artifactConstainer.getArtifacts(searchingDirectory, artifactName, processingType);
        Path targetPomDir = Paths.get(pomInstallationDirectory);
        Path targetJarDir = Paths.get(jarInstallationDirectory);

        try {
            if (!Files.exists(targetPomDir) && Files.isWritable(targetPomDir.getParent())) {
                Files.createDirectories(targetPomDir);
                logger.info("Created target directory: {}", targetPomDir);
            } else if (!Files.isWritable(targetPomDir)) {
                logger.error("Wrong access rights for target directory: {}", targetPomDir);
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create target POM directory", e);
        }

        try {
            if (!Files.exists(targetJarDir) && Files.isWritable(targetJarDir.getParent())) {
                Files.createDirectories(targetJarDir);
                logger.info("Created target directory: {}", targetJarDir);
            } else if (!Files.isWritable(targetJarDir)) {
                logger.error("Wrong access rights for target directory: {}", targetJarDir);
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create target POM directory", e);
        }

        Map<Path, Model> pomModels = new HashMap<>();

        Set<Path> processedJars = new HashSet<>();

        for (Map.Entry<String, Path> entry : artifactsMap.entrySet()) {
            Path pomPath = Paths.get(entry.getKey());

            try {
                Model model = readPomModel(pomPath);
                pomModels.put(pomPath, model);
            } catch (IOException | XmlPullParserException e) {
                logger.error("Failed to read POM file: {}", pomPath, e);
            }
        }

        Map<Path, Path> mainPomForJar = new HashMap<>();

        for (Map.Entry<String, Path> entry : artifactsMap.entrySet()) {
            Path pomPath = Paths.get(entry.getKey());
            Path jarPath = entry.getValue();
            Model model = pomModels.get(pomPath);

            if (model != null && !"pom".equals(model.getPackaging())) {
                mainPomForJar.put(jarPath, pomPath);
            }
        }

        for (Map.Entry<String, Path> entry : artifactsMap.entrySet()) {
            Path pomPath = Paths.get(entry.getKey());
            Model model = pomModels.get(pomPath);

            if (model != null && model.getArtifactId() != null) {
                String newPomName = model.getArtifactId() + ".pom";
                Path targetPom = targetPomDir.resolve(newPomName);

                try {
                    Files.copy(pomPath, targetPom, StandardCopyOption.REPLACE_EXISTING);
                    logger.info("Copied POM: {} -> {}", pomPath, targetPom);
                } catch (IOException e) {
                    logger.error("Failed to copy POM: {}", pomPath, e);
                }
            }
        }

        for (Map.Entry<String, Path> entry : artifactsMap.entrySet()) {
            Path jarPath = entry.getValue();

           if (processedJars.contains(jarPath)) {
                continue;
            }
            processedJars.add(jarPath);

            Path mainPomPath = mainPomForJar.get(jarPath);

            if (mainPomPath != null) {
                Model model = pomModels.get(mainPomPath);
                if (model != null && model.getArtifactId() != null) {
                    String newJarName = model.getArtifactId() + ".jar";
                    Path targetJar = targetJarDir.resolve(newJarName);

                    try {
                        Files.copy(jarPath, targetJar, StandardCopyOption.REPLACE_EXISTING);
                        logger.info("Copied JAR: {} -> {} (based on POM: {})", jarPath, targetJar, mainPomPath);
                    } catch (IOException e) {
                        logger.error("Failed to copy JAR: {}", jarPath, e);
                    }
                }
            } else {

                Path firstPomPath = Paths.get(entry.getKey());
                Model model = pomModels.get(firstPomPath);

                if (model != null && model.getArtifactId() != null) {
                    String newJarName = model.getArtifactId() + ".jar";
                    Path targetJar = targetJarDir.resolve(newJarName);

                    try {
                        Files.copy(jarPath, targetJar, StandardCopyOption.REPLACE_EXISTING);
                        logger.warn("\nCopied JAR without main POM: {} -> {} (based on POM: {})",
                                jarPath, targetJar, firstPomPath);
                    } catch (IOException e) {
                        logger.error("Failed to copy JAR: {}", jarPath, e);
                    }
                }
            }
        }
    }

    private Model readPomModel(Path pomPath) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try (FileInputStream fis = new FileInputStream(pomPath.toFile())) {
            return reader.read(fis);
        }
    }
}