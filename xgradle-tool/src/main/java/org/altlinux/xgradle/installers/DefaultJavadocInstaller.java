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
package org.altlinux.xgradle.installers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.ProcessingType;
import org.altlinux.xgradle.ToolConfig;
import org.altlinux.xgradle.api.collectors.ArtifactCollector;
import org.altlinux.xgradle.api.installers.JavadocInstaller;

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
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

/**
 * Default implementation of JavadocInstaller for installing Javadoc artifacts.
 * Handles copying and renaming of Javadoc JAR files based on artifact metadata.
 *
 * @author Ivan Khanas
 */
@Singleton
public class DefaultJavadocInstaller implements JavadocInstaller {
    private static final Logger logger = LoggerFactory.getLogger("XGradleLogger");
    private final ArtifactCollector artifactCollector;
    private final ToolConfig toolConfig;

    /**
     * Constructs a new DefaultJavadocInstaller with required dependencies.
     *
     * @param artifactCollector collector for retrieving Javadoc artifacts
     * @param toolConfig configuration for the tool
     */
    @Inject
    public DefaultJavadocInstaller(ArtifactCollector artifactCollector, ToolConfig toolConfig) {
        this.artifactCollector = artifactCollector;
        this.toolConfig = toolConfig;
    }

    /**
     * Installs Javadoc artifacts to the specified target directory.
     * Copies Javadoc JAR files with standardized naming based on artifactId.
     * Creates or updates .mfiles-javadoc file in current directory with target paths.
     *
     * @param searchingDir the directory to search for Javadoc artifacts
     * @param artifactNames optional list of artifact names to filter by
     * @param jarInstallationDir target directory for Javadoc JAR files
     */
    @Override
    public void installJavadoc(String searchingDir, Optional<List<String>> artifactNames, String jarInstallationDir) {
        HashMap<String, Path> javadocMap = artifactCollector.collect(searchingDir, artifactNames, ProcessingType.JAVADOC);

        logger.debug("Total Javadoc artifacts after collection and filtering: {}", javadocMap.size());

        if (javadocMap.isEmpty()) {
            logger.info("No Javadoc artifacts found after filtering, skipping installation");
            return;
        }

        Path targetDir = Paths.get(jarInstallationDir);

        try {
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
                logger.debug("Created target directory: {}", targetDir);
            } else if (!Files.isWritable(targetDir)) {
                logger.error("Wrong access rights for target directory: {}", targetDir);
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create target directory", e);
        }

        int copiedCount = 0;

        for (HashMap.Entry<String, Path> entry : javadocMap.entrySet()) {
            Path pomPath = Paths.get(entry.getKey());
            Path javadocPath = entry.getValue();

            try {
                String artifactId = extractArtifactIdFromPom(pomPath);
                if (artifactId != null) {
                    String newJavadocName = artifactId + "-javadoc.jar";
                    Path targetJavadocPath = targetDir.resolve(newJavadocName);

                    Files.copy(javadocPath, targetJavadocPath, StandardCopyOption.REPLACE_EXISTING);
                    logger.info("Copied Javadoc: {} -> {}", javadocPath.getFileName(), targetJavadocPath);
                    copiedCount++;
                } else {
                    logger.warn("Could not extract artifactId from POM: {}, skipping Javadoc: {}",
                            pomPath, javadocPath.getFileName());
                }
            } catch (Exception e) {
                logger.error("Failed to process Javadoc artifact: {}", javadocPath, e);
            }
        }

        String pathForMfiles = preparePathForMfiles(jarInstallationDir);
        updateMfilesJavadocFile(pathForMfiles);

        logger.debug("Successfully installed {} Javadoc artifacts to {}", copiedCount, targetDir);
    }

    /**
     * Prepares the path for writing to .mfiles-javadoc by stripping the install prefix if specified.
     *
     * @param originalPath the original installation path
     * @return the prepared path with prefix stripped if applicable
     */
    private String preparePathForMfiles(String originalPath) {
        if (toolConfig.getInstallPrefix() != null && !toolConfig.getInstallPrefix().isEmpty()) {
            String installPrefix = toolConfig.getInstallPrefix();

            if (originalPath.startsWith(installPrefix)) {
                String strippedPath = originalPath.substring(installPrefix.length());
                logger.debug("Stripped install prefix '{}' from path '{}', result: '{}'",
                        installPrefix, originalPath, strippedPath);
                return strippedPath;
            } else {
                logger.debug("Install prefix '{}' not found in path '{}', using original path",
                        installPrefix, originalPath);
            }
        }

        return originalPath;
    }

    /**
     * Updates .mfiles-javadoc file in current directory.
     * If file doesn't exist, creates it with the target path.
     * If file exists and path is not present, appends the path on a new line.
     * If file exists and path is already present, does nothing.
     *
     * @param targetPath the target path to add to .mfiles-javadoc file
     */
    private void updateMfilesJavadocFile(String targetPath) {
        try {
            Path currentDir = Paths.get(".").toAbsolutePath();
            Path mfilesJavadoc = currentDir.resolve(".mfiles-javadoc");

            if (!Files.exists(mfilesJavadoc)) {

                Files.write(mfilesJavadoc, targetPath.getBytes());
                logger.info("Created .mfiles-javadoc file in {} with content: {}",
                        currentDir, targetPath);
            } else {

                Set<String> existingPaths = new HashSet<>();
                List<String> lines = Files.readAllLines(mfilesJavadoc);

                for (String line : lines) {
                    String trimmedLine = line.trim();
                    if (!trimmedLine.isEmpty()) {
                        existingPaths.add(trimmedLine);
                    }
                }

                if (!existingPaths.contains(targetPath)) {

                    Files.write(mfilesJavadoc, System.lineSeparator().getBytes(), StandardOpenOption.APPEND);
                    Files.write(mfilesJavadoc, targetPath.getBytes(), StandardOpenOption.APPEND);

                    logger.info("Appended new path to .mfiles-javadoc file: {}", targetPath);
                } else {
                    logger.info("Path already exists in .mfiles-javadoc file: {}", targetPath);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to update .mfiles-javadoc file", e);
        }
    }

    /**
     * Extracts artifactId from POM file.
     *
     * @param pomPath path to the POM file
     * @return artifactId or null if extraction fails
     */
    private String extractArtifactIdFromPom(Path pomPath) {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try (FileInputStream fis = new FileInputStream(pomPath.toFile())) {
            Model model = reader.read(fis);
            String artifactId = model.getArtifactId();

            if (artifactId == null && model.getParent() != null) {
                artifactId = model.getParent().getArtifactId();
            }

            return artifactId;
        } catch (IOException | XmlPullParserException e) {
            logger.error("Error reading POM file: {}", pomPath, e);
            return null;
        }
    }
}