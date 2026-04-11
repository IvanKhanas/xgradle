/*
 * Copyright 2025 BaseALT Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.altlinux.xgradle.impl.installers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.impl.enums.ProcessingType;
import org.altlinux.xgradle.interfaces.containers.ArtifactContainer;
import org.altlinux.xgradle.interfaces.installers.ArtifactsInstaller;
import org.altlinux.xgradle.interfaces.resolvers.PluginPomChainResolver;
import org.altlinux.xgradle.interfaces.resolvers.PluginPomChainResult;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.slf4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;

/**
 * Default implementation of ArtifactsInstaller for Gradle plugin artifacts.
 * Implements {@link ArtifactsInstaller}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultPluginArtifactsInstaller implements ArtifactsInstaller {

    private final ArtifactContainer artifactContainer;
    private final PluginPomChainResolver pomChainResolver;
    private final Logger logger;

    @Inject
    DefaultPluginArtifactsInstaller(
            ArtifactContainer artifactContainer,
            PluginPomChainResolver pomChainResolver,
            Logger logger
    ) {
        this.artifactContainer = artifactContainer;
        this.pomChainResolver = pomChainResolver;
        this.logger = logger;
    }

    @Override
    public void install(
            String searchingDirectory,
            List<String> artifactNames,
            String pomInstallationDirectory,
            String jarInstallationDirectory,
            ProcessingType processingType
    ) {
        HashMap<String, Path> artifactsMap = artifactContainer.getArtifacts(
                searchingDirectory,
                artifactNames,
                processingType
        );
        Path targetPomDir = Paths.get(pomInstallationDirectory);
        Path targetJarDir = Paths.get(jarInstallationDirectory);

        try {
            if (!Files.exists(targetPomDir) && Files.isWritable(targetPomDir.getParent())) {
                Files.createDirectories(targetPomDir);
                logger.info("Created POM target directory: {}", targetPomDir);
            } else if (!Files.isWritable(targetPomDir)) {
                logger.error("Wrong access rights for POM target directory: {}", targetPomDir);
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create target POM directory", e);
        }

        try {
            if (!Files.exists(targetJarDir) && Files.isWritable(targetJarDir.getParent())) {
                Files.createDirectories(targetJarDir);
                logger.info("Created JAR target directory: {}", targetJarDir);
            } else if (!Files.isWritable(targetJarDir)) {
                logger.error("Wrong access rights for JAR target directory: {}", targetJarDir);
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create target POM directory", e);
        }

        PluginPomChainResult pomChain = pomChainResolver.resolve(
                searchingDirectory,
                artifactNames,
                artifactsMap
        );
        Map<Path, Model> pomModels = pomChain.getPomModels();

        Set<Path> processedJars = new HashSet<>();

        Map<Path, Path> mainPomForJar = new HashMap<>();

        artifactsMap.entrySet().forEach(entry -> {
            Path pomPath = Paths.get(entry.getKey());
            Model model = pomModels.get(pomPath);
            if (model != null && !"pom".equals(model.getPackaging())) {
                mainPomForJar.put(entry.getValue(), pomPath);
            }
        });

        Set<Path> pomPathsToCopy = pomChain.getPomPaths();

        pomPathsToCopy.forEach(pomPath -> {
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
        });

        artifactsMap.entrySet().forEach(entry -> {
            Path jarPath = entry.getValue();
            if (!processedJars.add(jarPath)) {
                return;
            }

            Path mainPomPath = mainPomForJar.get(jarPath);

            if (mainPomPath != null) {
                Model model = pomModels.get(mainPomPath);
                if (model != null && model.getArtifactId() != null) {
                    String newJarName = model.getArtifactId() + ".jar";
                    Path targetJar = targetJarDir.resolve(newJarName);
                    try {
                        Files.copy(jarPath, targetJar, StandardCopyOption.REPLACE_EXISTING);
                        logger.info("Copied JAR from main POM: {} -> {} (POM: {})",
                                jarPath, targetJar, mainPomPath);
                    } catch (IOException e) {
                        logger.error("Failed to copy JAR from main POM: {}", jarPath, e);
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
                        logger.warn("Copied JAR without main POM: {} -> {} (POM: {})",
                                jarPath, targetJar, firstPomPath);
                    } catch (IOException e) {
                        logger.error("Failed to copy JAR without main POM: {}", jarPath, e);
                    }
                }
            }
        });

        pomPathsToCopy.forEach(pomPath -> {
            Model model = pomModels.get(pomPath);
            if (model == null || model.getArtifactId() == null) {
                return;
            }

            Path jarPath = resolveJarPath(pomPath, model);
            if (jarPath == null || !processedJars.add(jarPath)) {
                return;
            }

            Path targetJar = targetJarDir.resolve(model.getArtifactId() + ".jar");
            try {
                Files.copy(jarPath, targetJar, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Copied JAR from POM-only entry: {} -> {} (POM: {})", jarPath, targetJar, pomPath);
            } catch (IOException e) {
                logger.error("Failed to copy JAR from POM-only entry: {}", jarPath, e);
            }
        });
    }

    private Model readPomModel(Path pomPath) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try (FileInputStream fis = new FileInputStream(pomPath.toFile())) {
            return reader.read(fis);
        }
    }

    private Path resolveJarPath(Path pomPath, Model model) {
        String artifactId = model.getArtifactId();
        String version = model.getVersion();
        if (version == null && model.getParent() != null) {
            version = model.getParent().getVersion();
        }

        Path dir = pomPath.getParent();
        if (artifactId == null || dir == null) {
            return null;
        }

        if (version != null) {
            Path versioned = dir.resolve(artifactId + "-" + version + ".jar");
            if (Files.exists(versioned)) {
                return versioned;
            }
        }

        Path unversioned = dir.resolve(artifactId + ".jar");
        if (Files.exists(unversioned)) {
            return unversioned;
        }

        return null;
    }
}
