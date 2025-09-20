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

import org.altlinux.xgradle.api.containers.PomContainer;

import org.altlinux.xgradle.api.parsers.PomParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

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

public class ConcurrentLibraryPomParser implements PomParser {
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentLibraryPomParser.class);
    private final PomContainer pomContainer;

    @Inject
    public ConcurrentLibraryPomParser(PomContainer pomContainer){
        this.pomContainer = pomContainer;
    }

    @Override
    public ConcurrentLibraryPomParser parsePoms() {
        return this;
    }

    @Override
    public HashMap<String, Path> getArtifactCoords(String searchingDir, Optional<String> artifactName) {
        Collection<Path> pomPaths;

        if (artifactName.isPresent()) {
            pomPaths = pomContainer.getSelectedPomPaths(searchingDir, artifactName.get());
        } else {
            pomPaths = pomContainer.getAllPomPaths(searchingDir);
        }
        int threadCount = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        ConcurrentHashMap<String, Path> artifactCoordinatesMap = new ConcurrentHashMap<>();

        try {
            CompletableFuture.allOf(pomPaths.stream()
                    .map(pomPath -> CompletableFuture.runAsync(() -> {
                        try {
                            Path jarPath = getJarPathFromPom(pomPath);
                            if(isSnapshotJar(jarPath)) {
                                logger.info("Found snapshot jar at: \n{}\t SKIPPING :)", jarPath);
                            }
                            if(isJarExists(jarPath) && !isSnapshotJar(jarPath)) {
                                artifactCoordinatesMap.put(pomPath.toString(), jarPath);
                            }
                        } catch (Exception e) {
                            logger.error("Failed to parse: {}", pomPath.toString(), e);
                        }
                    }, executor)).toArray(CompletableFuture[]::new)).join();
        } finally {
            executor.shutdown();
        }
        return new HashMap<>(artifactCoordinatesMap);
    }

    private Path getJarPathFromPom(Path pomPath) {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        String artifactId;
        String version;

        try (FileInputStream fis = new FileInputStream(pomPath.toFile())) {
            Model model = reader.read(fis);
            artifactId = model.getArtifactId();
            version = model.getVersion();

            if (version == null && model.getParent() != null) {
                version = model.getParent().getVersion();
            }

            if (artifactId == null || version == null) {
                throw new RuntimeException("Could not determine artifactId or version for POM: " + pomPath);
            }

            String jarFileName = artifactId + "-" + version + ".jar";

            return pomPath.getParent().resolve(jarFileName);

        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException("Error reading POM file: " + pomPath, e);
        }
    }

    private boolean isJarExists (Path jarPath) {
        return jarPath.toFile().exists();
    }

    private boolean isSnapshotJar(Path jarPath) {
        return jarPath.toFile().toString().toLowerCase().contains("-snapshot");
    }
}