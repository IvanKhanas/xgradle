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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parser for Gradle plugin POM files with duplicate prevention.
 * Handles complex plugin dependencies and analyzes POM dependencies to find related artifacts.
 * Prevents duplicate processing of artifacts with same coordinates.
 *
 * @author Ivan Khanas
 */
@Singleton
public class DefaultPluginPomParser implements PomParser<HashMap<String, Path>> {
    private static final Logger logger = LoggerFactory.getLogger("XGradleLogger");

    private final PomContainer pomContainer;
    private final ArtifactCache artifactCache;

    /**
     * Constructs a new PluginPomsParser with required dependencies.
     *
     * @param pomContainer container for POM file management
     * @param artifactCache cache for tracking processed artifacts
     */
    @Inject
    public DefaultPluginPomParser(PomContainer pomContainer, ArtifactCache artifactCache) {
        this.pomContainer = pomContainer;
        this.artifactCache = artifactCache;
    }

    /**
     * Retrieves plugin artifact coordinates from the specified directory.
     * Analyzes POM dependencies to find all related artifacts for plugin installation.
     * Prevents duplicate processing of artifacts with same coordinates.
     *
     * @param searchingDir the directory to search for plugin POM files
     * @param artifactNames optional list of artifact names to filter by
     * @return map of POM file paths to corresponding JAR file paths
     * @throws RuntimeException if an error occurs during POM file processing
     */
    @Override
    public HashMap<String, Path> getArtifactCoords(String searchingDir, Optional<List<String>> artifactNames) {
        if (artifactNames == null || !artifactNames.isPresent()) {
            return new HashMap<>();
        }

        HashMap<String, Path> result = new HashMap<>();
        List<String> artifactNameValues = artifactNames.get();

        Collection<Path> allPomPaths = pomContainer.getAllPoms(searchingDir);

        List<Path> filteredPomPaths = allPomPaths.stream()
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    return artifactNameValues.stream()
                            .anyMatch(fileName::startsWith);
                })
                .collect(Collectors.toList());

        if (filteredPomPaths.isEmpty()) {
            return result;
        }

        for (Path pomPath : filteredPomPaths) {
            try {
                Model model = readModel(pomPath);
                DefaultArtifactCoordinates coordinates = extractCoordinates(model);

                if (artifactCache.contains(coordinates)) {
                    DefaultArtifactData existing = (DefaultArtifactData) artifactCache.get(coordinates);
                    logger.warn("Skipping duplicate plugin artifact: {} (already processed from: {})",
                            coordinates, existing.getPomPath());
                    continue;
                }

                if ("pom".equals(model.getPackaging())) {
                    analyzePomDependencies(searchingDir, pomPath, model, result, coordinates);
                } else {
                    Path jarPath = findJarForPom(pomPath, model);
                    if (jarPath != null && Files.exists(jarPath)) {
                        DefaultArtifactData artifactData = new DefaultArtifactData(coordinates, model, pomPath, jarPath);
                        if (artifactCache.add(artifactData)) {
                            result.put(pomPath.toString(), jarPath);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error processing plugin POM file: {}", pomPath, e);
            }
        }

        logger.info("Processed {} unique plugin artifacts", result.size());
        return result;
    }

    /**
     * Analyzes POM dependencies to find all related artifacts for plugin installation.
     * Recursively processes POM dependencies to build complete artifact map.
     * Prevents duplicate processing of artifacts with same coordinates.
     *
     * @param searchingDir the directory to search for dependency POM files
     * @param pomPath path to the POM file being analyzed
     * @param model the Maven model of the POM file
     * @param result map to store the resulting artifact coordinates
     * @param parentCoordinates coordinates of the parent artifact
     */
    private void analyzePomDependencies(String searchingDir, Path pomPath, Model model,
                                        HashMap<String, Path> result, DefaultArtifactCoordinates parentCoordinates) {
        if (model.getDependencies() == null) {
            return;
        }

        Collection<Path> allPomPaths = pomContainer.getAllPoms(searchingDir);
        Set<String> allPomFileNames = allPomPaths.stream()
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toSet());

        for (Dependency dependency : model.getDependencies()) {
            String dependencyType = dependency.getType();
            String dependencyArtifactId = dependency.getArtifactId();
            String dependencyVersion = dependency.getVersion();

            if (dependencyArtifactId == null || dependencyVersion == null) {
                continue;
            }

            String expectedPomName = dependencyArtifactId + "-" + dependencyVersion + ".pom";

            if ("pom".equals(dependencyType) && allPomFileNames.contains(expectedPomName)) {
                Path dependencyPomPath = allPomPaths.stream()
                        .filter(path -> path.getFileName().toString().equals(expectedPomName))
                        .findFirst()
                        .orElse(null);

                if (dependencyPomPath != null) {
                    try {
                        Model dependencyModel = readModel(dependencyPomPath);
                        DefaultArtifactCoordinates depCoordinates = extractCoordinates(dependencyModel);

                        if (!artifactCache.contains(depCoordinates)) {
                            analyzePomDependencies(searchingDir, dependencyPomPath, dependencyModel,
                                    result, depCoordinates);
                        } else {
                            logger.debug("Skipping duplicate dependency: {}", depCoordinates);
                        }
                    } catch (Exception e) {
                        logger.error("Error processing dependency POM: {}", dependencyPomPath, e);
                    }
                }
            } else if (!"pom".equals(dependencyType)) {
                String jarFileName = dependencyArtifactId + "-" + dependencyVersion + ".jar";
                Path jarPath = pomPath.getParent().resolve(jarFileName);

                if (Files.exists(jarPath)) {
                    DefaultArtifactCoordinates coordinates = new DefaultArtifactCoordinates(
                            dependency.getGroupId(), dependencyArtifactId, dependencyVersion);

                    if (!artifactCache.contains(coordinates)) {
                        try {
                            Model jarModel = createModelForDependency(dependency);
                            DefaultArtifactData artifactData = new DefaultArtifactData(coordinates, jarModel, pomPath, jarPath);
                            if (artifactCache.add(artifactData)) {
                                result.put(pomPath.toString(), jarPath);
                            }
                        } catch (Exception e) {
                            logger.error("Error creating model for dependency: {}", coordinates, e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Extracts coordinates from Maven model.
     *
     * @param model the Maven model
     * @return artifact coordinates
     */
    private DefaultArtifactCoordinates extractCoordinates(Model model) {
        String groupId = model.getGroupId();
        String artifactId = model.getArtifactId();
        String version = model.getVersion();

        if (groupId == null && model.getParent() != null) {
            groupId = model.getParent().getGroupId();
        }
        if (version == null && model.getParent() != null) {
            version = model.getParent().getVersion();
        }

        return new DefaultArtifactCoordinates(groupId, artifactId, version);
    }

    /**
     * Creates a minimal Maven model for a dependency.
     *
     * @param dependency the dependency
     * @return created model
     */
    private Model createModelForDependency(Dependency dependency) {
        Model model = new Model();
        model.setGroupId(dependency.getGroupId());
        model.setArtifactId(dependency.getArtifactId());
        model.setVersion(dependency.getVersion());
        return model;
    }

    /**
     * Finds the corresponding JAR file for a POM file.
     *
     * @param pomPath path to the POM file
     * @param model the Maven model of the POM file
     * @return path to the corresponding JAR file, or null if not found
     */
    private Path findJarForPom(Path pomPath, Model model) {
        String artifactId = model.getArtifactId();
        String version = model.getVersion();

        if (artifactId != null && version != null) {
            String jarFileName = artifactId + "-" + version + ".jar";
            Path jarPath = pomPath.getParent().resolve(jarFileName);

            if (Files.exists(jarPath)) {
                return jarPath;
            }
        }

        return null;
    }

    /**
     * Reads and parses a POM file into a Maven model.
     *
     * @param pomPath path to the POM file
     * @return parsed Maven model
     * @throws IOException if an I/O error occurs
     * @throws XmlPullParserException if the POM file cannot be parsed
     */
    private Model readModel(Path pomPath) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try (FileInputStream fis = new FileInputStream(pomPath.toFile())) {
            return reader.read(fis);
        }
    }
}