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

import org.altlinux.xgradle.ToolConfig;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.List;

public class ConcurrentLibraryPomParser implements PomParser<HashMap<String, Path>> {
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentLibraryPomParser.class);
    private final PomContainer pomContainer;
    private final ToolConfig toolConfig;

    @Inject
    public ConcurrentLibraryPomParser(PomContainer pomContainer, ToolConfig toolConfig) {
        this.pomContainer = pomContainer;
        this.toolConfig = toolConfig;
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

                            if(jarPath!=null && isJarExists(jarPath)) {
                                artifactCoordinatesMap.put(pomPath.toString(), jarPath);
                            }
                        } catch (Exception e) {
                            logger.error("Failed to parse: {}", pomPath.toString(), e);
                        }
                    }, executor)).toArray(CompletableFuture[]::new)).join();
        } finally {
            executor.shutdown();
        }

        return excludeArtifacts(toolConfig.getExcludedArtifacts(), artifactCoordinatesMap);
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

            if(!toolConfig.isAllowSnapshots()) {
                if (version.toLowerCase().contains("snapshot")) {
                    logger.warn("Found snapshot POM: " + pomPath + "\tSKIPPING");
                    return null;
                }
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

    private HashMap<String, Path> excludeArtifacts(List<String> excludedArtifacts, ConcurrentHashMap<String, Path> artifactCoordinatesMap) {
        if (!toolConfig.getExcludedArtifacts().isEmpty()) {
        HashMap<String, Path> filteredMap = artifactCoordinatesMap.entrySet().stream()
                .filter(entry -> {
                    Path pomPath = Path.of(entry.getKey());
                    String filename = pomPath.getFileName().toString();

                    return excludedArtifacts.stream()
                            .noneMatch(filename::startsWith);
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, replacement) -> existing,
                        HashMap::new
                ));

        return new HashMap<>(filteredMap);
        } else {
            return new HashMap<>(artifactCoordinatesMap);
        }
    }
}