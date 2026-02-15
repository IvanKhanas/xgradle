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
package org.altlinux.xgradle.impl.parsers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.caches.ArtifactCache;
import org.altlinux.xgradle.interfaces.containers.PomContainer;
import org.altlinux.xgradle.interfaces.model.ArtifactCoordinates;
import org.altlinux.xgradle.interfaces.model.ArtifactData;
import org.altlinux.xgradle.interfaces.model.ArtifactFactory;
import org.altlinux.xgradle.interfaces.parsers.PomParser;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.slf4j.Logger;

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
 * Implements {@link PomParser}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class ConcurrentBomParser implements PomParser<Set<Path>> {

    private final ArtifactFactory artifactFactory;
    private final PomContainer pomContainer;
    private final ArtifactCache artifactCache;
    private final Logger logger;

    @Inject
    ConcurrentBomParser(
            PomContainer pomContainer,
            ArtifactCache artifactCache,
            ArtifactFactory artifactFactory,
            Logger logger
    ) {
        this.pomContainer = pomContainer;
        this.artifactCache = artifactCache;
        this.artifactFactory = artifactFactory;
        this.logger = logger;
    }

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

    private void processBomFile(Path pomPath, Set<Path> bomSet) {
        if (!isBom(pomPath)) {
            return;
        }

        try {
            ArtifactData artifactData = extractArtifactData(pomPath);
            if (artifactData == null) {
                return;
            }

            if (artifactCache.add(artifactData)) {
                bomSet.add(pomPath);
                logger.debug("Added BOM: {}", artifactData.getCoordinates());
            } else {
                ArtifactData existing = artifactCache.get(artifactData.getCoordinates());
                logger.warn("Skipping duplicate BOM: {} (already processed from: {})",
                        artifactData.getCoordinates(), existing.getPomPath());
            }
        } catch (Exception e) {
            logger.error("Failed to process BOM file: {}", pomPath, e);
        }
    }

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

    private ArtifactData extractArtifactData(Path pomPath) {
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
                logger.debug("Incomplete coordinates in BOM POM: {}", pomPath);
            }

            ArtifactCoordinates coordinates = artifactFactory.coordinates(groupId, artifactId, version);
            return artifactFactory.data(coordinates, model, pomPath, null);

        } catch (IOException | XmlPullParserException e) {
            logger.error("Error reading BOM POM file: {}", pomPath, e);
            return null;
        }
    }
}
