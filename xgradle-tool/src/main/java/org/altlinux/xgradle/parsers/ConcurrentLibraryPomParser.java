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
package org.altlinux.xgradle.parsers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.api.model.ArtifactCache;
import org.altlinux.xgradle.api.containers.PomContainer;
import org.altlinux.xgradle.api.parsers.PomParser;
import org.altlinux.xgradle.model.DefaultArtifactCoordinates;
import org.altlinux.xgradle.model.DefaultArtifactData;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

/**
 * Concurrent parser for library POM files with duplicate prevention.
 * Uses multi-threading to improve performance when processing large numbers of POM files.
 * Prevents duplicate processing of artifacts with same coordinates.
 *
 * @author Ivan Khanas
 */
@Singleton
public class ConcurrentLibraryPomParser implements PomParser<HashMap<String, Path>> {
    private static final Logger logger = LoggerFactory.getLogger("XGradleLogger");
    private final PomContainer pomContainer;
    private final ArtifactCache artifactCache;

    /**
     * Constructs a new ConcurrentLibraryPomParser with required dependencies.
     *
     * @param pomContainer container for POM file management
     * @param artifactCache cache for tracking processed artifacts
     */
    @Inject
    public ConcurrentLibraryPomParser(PomContainer pomContainer, ArtifactCache artifactCache) {
        this.pomContainer = pomContainer;
        this.artifactCache = artifactCache;
    }

    /**
     * Retrieves artifact coordinates from the specified directory using concurrent processing.
     * For each POM file, finds the corresponding JAR file and verifies its existence.
     * Prevents duplicate processing of artifacts with same coordinates.
     *
     * @param searchingDir the directory to search for POM files
     * @param artifactNames optional list of artifact names to filter by
     * @return map of POM file paths to corresponding JAR file paths
     * @throws RuntimeException if an error occurs during POM file processing
     */
    @Override
    public HashMap<String, Path> getArtifactCoords(String searchingDir, Optional<List<String>> artifactNames) {
        Collection<Path> pomPaths;

        if (artifactNames.isPresent()) {
            pomPaths = pomContainer.getSelectedPoms(searchingDir, artifactNames.get());
        } else {
            pomPaths = pomContainer.getAllPoms(searchingDir);
        }

        int threadCount = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        ConcurrentHashMap<String, Path> artifactCoordinatesMap = new ConcurrentHashMap<>();

        try {
            CompletableFuture.allOf(pomPaths.stream()
                    .map(pomPath -> CompletableFuture.runAsync(() -> {
                        try {
                            processPomFile(pomPath, artifactCoordinatesMap);
                        } catch (Exception e) {
                            logger.error("Error processing POM file: {}", pomPath, e);
                        }
                    }, executor)).toArray(CompletableFuture[]::new)).join();
        } finally {
            executor.shutdown();
        }

        logger.info("Processed {} unique artifacts from {} POM files",
                artifactCoordinatesMap.size(), pomPaths.size());
        return new HashMap<>(artifactCoordinatesMap);
    }

    /**
     * Processes a single POM file and adds it to the result map if not duplicate.
     *
     * @param pomPath path to the POM file
     * @param artifactCoordinatesMap map to store the results
     */
    private void processPomFile(Path pomPath, ConcurrentHashMap<String, Path> artifactCoordinatesMap) {
        try {
            DefaultArtifactData artifactData = extractArtifactData(pomPath);
            if (artifactData == null) {
                return;
            }

            if (artifactCache.add(artifactData)) {
                if (artifactData.getJarPath() != null && artifactData.getJarPath().toFile().exists()) {
                    artifactCoordinatesMap.put(pomPath.toString(), artifactData.getJarPath());
                    logger.debug("Added artifact: {}", artifactData.getCoordinates());
                }
            } else {
                DefaultArtifactData existing = (DefaultArtifactData) artifactCache.get(artifactData.getCoordinates());
                logger.warn("Skipping duplicate artifact: {} (already processed from: {})",
                        artifactData.getCoordinates(), existing.getPomPath());
            }
        } catch (Exception e) {
            logger.error("Failed to process POM file: {}", pomPath, e);
        }
    }

    /**
     * Extracts artifact data from POM file including coordinates and JAR path.
     *
     * @param pomPath path to the POM file
     * @return artifact data or null if extraction fails
     */
    private DefaultArtifactData extractArtifactData(Path pomPath) {
        MavenXpp3Reader reader = new MavenXpp3Reader();

        try (FileInputStream fis = new FileInputStream(pomPath.toFile())) {
            Model model = reader.read(fis);

            String groupId = model.getGroupId();
            String artifactId = model.getArtifactId();
            String version = model.getVersion();

            if (groupId == null && model.getParent() != null) {
                groupId = model.getParent().getGroupId();
            }
            if (version == null && model.getParent() != null) {
                version = model.getParent().getVersion();
            }

            if (groupId == null || artifactId == null || version == null) {
                logger.warn("Incomplete coordinates in POM: {}. GroupId: {}, ArtifactId: {}, Version: {}",
                        pomPath, groupId, artifactId, version);
                return null;
            }

            DefaultArtifactCoordinates coordinates = new DefaultArtifactCoordinates(groupId, artifactId, version);
            Path jarPath = getJarPathFromModel(pomPath, model);

            return new DefaultArtifactData(coordinates, model, pomPath, jarPath);

        } catch (IOException | XmlPullParserException e) {
            logger.error("Error reading POM file: {}", pomPath, e);
            return null;
        }
    }

    /**
     * Determines JAR file path from POM model.
     *
     * @param pomPath path to the POM file
     * @param model the Maven model
     * @return path to the corresponding JAR file
     */
    private Path getJarPathFromModel(Path pomPath, Model model) {
        String artifactId = model.getArtifactId();
        String version = model.getVersion();

        if (version == null && model.getParent() != null) {
            version = model.getParent().getVersion();
        }

        if (artifactId == null || version == null) {
            throw new RuntimeException("Could not determine artifactId or version for POM: " + pomPath);
        }

        String jarFileName = artifactId + "-" + version + ".jar";
        return pomPath.getParent().resolve(jarFileName);
    }
}