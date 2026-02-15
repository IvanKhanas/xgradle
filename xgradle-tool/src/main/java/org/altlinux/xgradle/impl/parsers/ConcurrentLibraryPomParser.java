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
 * Implements {@link PomParser<HashMap<String} and {@link Path>>}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class ConcurrentLibraryPomParser implements PomParser<HashMap<String, Path>> {

    private final ArtifactFactory artifactFactory;
    private final PomContainer pomContainer;
    private final ArtifactCache artifactCache;
    private final Logger logger;

    @Inject
    ConcurrentLibraryPomParser(
            PomContainer pomContainer,
            ArtifactCache artifactCache,
            ArtifactFactory artifactFactory,
            Logger logger
    ) {
        this.artifactFactory = artifactFactory;
        this.pomContainer = pomContainer;
        this.artifactCache = artifactCache;
        this.logger = logger;
    }

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

    private void processPomFile(Path pomPath, ConcurrentHashMap<String, Path> artifactCoordinatesMap) {
        try {
            ArtifactData artifactData = extractArtifactData(pomPath);
            if (artifactData == null) {
                return;
            }

            if (artifactCache.add(artifactData)) {
                if (artifactData.getJarPath() != null && artifactData.getJarPath().toFile().exists()) {
                    artifactCoordinatesMap.put(pomPath.toString(), artifactData.getJarPath());
                    logger.debug("Added artifact: {}", artifactData.getCoordinates());
                }
            } else {
                ArtifactData existing = artifactCache.get(artifactData.getCoordinates());
                logger.warn("Skipping duplicate artifact: {} (already processed from: {})",
                        artifactData.getCoordinates(), existing.getPomPath());
            }
        } catch (Exception e) {
            logger.error("Failed to process POM file: {}", pomPath, e);
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
                logger.debug("Incomplete coordinates in POM: {}. GroupId: {}, ArtifactId: {}, Version: {}",
                        pomPath, groupId, artifactId, version);
            }

            ArtifactCoordinates coordinates = artifactFactory.coordinates(groupId, artifactId, version);
            Path jarPath = getJarPathFromModel(pomPath, model);

            return artifactFactory.data(coordinates, model, pomPath, jarPath);

        } catch (IOException | XmlPullParserException e) {
            logger.error("Error reading POM file: {}", pomPath, e);
            return null;
        }
    }

    private Path getJarPathFromModel(Path pomPath, Model model) {
        String artifactId = model.getArtifactId();
        String version = model.getVersion();

        if (version == null && model.getParent() != null) {
            version = model.getParent().getVersion();
        }

        if (artifactId == null) {
            throw new RuntimeException("Could not determine artifactId: " + pomPath);
        }

        String jarFileName = artifactId + "-" + version + ".jar";
        return pomPath.getParent().resolve(jarFileName);
    }
}
