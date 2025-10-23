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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Parser for BOM (Bill of Materials) POM files with duplicate prevention.
 * Identifies BOM files by packaging type and dependency management section.
 * Prevents duplicate processing of artifacts with same coordinates.
 *
 * @author Ivan Khanas
 */
@Singleton
public class ConcurrentBomParser implements PomParser<Set<Path>> {
    private static final Logger logger = LoggerFactory.getLogger("XGradleLogger");

    private final PomContainer pomContainer;
    private final ArtifactCache artifactCache;

    /**
     * Constructs a new DefaultBomParser with required dependencies.
     *
     * @param pomContainer container for POM file management
     * @param artifactCache cache for tracking processed artifacts
     */
    @Inject
    public ConcurrentBomParser(PomContainer pomContainer, ArtifactCache artifactCache) {
        this.pomContainer = pomContainer;
        this.artifactCache = artifactCache;
    }

    /**
     * Retrieves BOM artifact coordinates from the specified directory.
     * Filters POM files to only include those with packaging type "pom" and dependency management.
     * Prevents duplicate processing of artifacts with same coordinates.
     *
     * @param searchingDir the directory to search for BOM files
     * @param artifactNames optional list of artifact names to filter by
     * @return set of BOM file paths that match the criteria
     * @throws RuntimeException if an error occurs during POM file parsing
     */
    @Override
    public Set<Path> getArtifactCoords(String searchingDir, Optional<List<String>> artifactNames) {
        Set<Path> pomPaths;

        if (artifactNames.isPresent()) {
            pomPaths = pomContainer.getSelectedPoms(searchingDir, artifactNames.get());
        } else {
            pomPaths = pomContainer.getAllPoms(searchingDir);
        }

        int threadCount = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Set<Path> bomSet = ConcurrentHashMap.newKeySet();

        try {
            CompletableFuture.allOf(pomPaths.stream()
                            .map(pomPath -> CompletableFuture.runAsync(() -> {
                                try {
                                    processBomFile(pomPath, bomSet);
                                } catch (Exception e) {
                                    logger.error("Error processing BOM file: {}", pomPath, e);
                                }
                            }, executor))
                            .toArray(CompletableFuture[]::new))
                    .join();
        } finally {
            executor.shutdown();
        }

        logger.info("Processed {} unique BOM artifacts from {} POM files",
                bomSet.size(), pomPaths.size());
        return bomSet;
    }

    /**
     * Processes a single BOM file and adds it to the result set if not duplicate.
     *
     * @param pomPath path to the POM file
     * @param bomSet set to store the results
     */
    private void processBomFile(Path pomPath, Set<Path> bomSet) {
        if (!isBom(pomPath)) {
            return;
        }

        try {
            DefaultArtifactData artifactData = extractArtifactData(pomPath);
            if (artifactData == null) {
                return;
            }

            if (artifactCache.add(artifactData)) {
                bomSet.add(pomPath);
                logger.debug("Added BOM: {}", artifactData.getCoordinates());
            } else {
                DefaultArtifactData existing = (DefaultArtifactData) artifactCache.get(artifactData.getCoordinates());
                logger.warn("Skipping duplicate BOM: {} (already processed from: {})",
                        artifactData.getCoordinates(), existing.getPomPath());
            }
        } catch (Exception e) {
            logger.error("Failed to process BOM file: {}", pomPath, e);
        }
    }

    /**
     * Checks if the POM file is a BOM (Bill of Materials).
     *
     * @param pomPath path to the POM file
     * @return true if the file is a BOM, false otherwise
     */
    private boolean isBom(Path pomPath) {
        MavenXpp3Reader reader = new MavenXpp3Reader();

        try (FileInputStream fis = new FileInputStream(String.valueOf(pomPath))) {
            Model model = reader.read(fis);
            return "pom".equals(model.getPackaging())
                    && model.getDependencyManagement() != null;
        } catch (IOException | XmlPullParserException e) {
            logger.error("Error checking BOM file: {}", pomPath, e);
            return false;
        }
    }

    /**
     * Extracts artifact data from POM file.
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
                logger.warn("Incomplete coordinates in BOM POM: {}", pomPath);
                return null;
            }

            DefaultArtifactCoordinates coordinates = new DefaultArtifactCoordinates(groupId, artifactId, version);
            return new DefaultArtifactData(coordinates, model, pomPath, null);

        } catch (IOException | XmlPullParserException e) {
            logger.error("Error reading BOM POM file: {}", pomPath, e);
            return null;
        }
    }
}