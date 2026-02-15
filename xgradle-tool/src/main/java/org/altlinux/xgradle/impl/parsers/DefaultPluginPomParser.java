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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.slf4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parser for Gradle plugin POM files with duplicate prevention.
 * Implements {@link PomParser<HashMap<String} and {@link Path>>}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultPluginPomParser implements PomParser<HashMap<String, Path>> {

    private final ArtifactFactory artifactFactory;
    private final PomContainer pomContainer;
    private final ArtifactCache artifactCache;
    private final Logger logger;

    @Inject
    DefaultPluginPomParser(
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
                .toList();

        if (filteredPomPaths.isEmpty()) {
            return result;
        }

        for (Path pomPath : filteredPomPaths) {
            try {
                Model model = readModel(pomPath);
                ArtifactCoordinates coordinates = extractCoordinates(model);

                if (artifactCache.contains(coordinates)) {
                    ArtifactData existing = artifactCache.get(coordinates);
                    logger.warn("Skipping duplicate plugin artifact: {} (already processed from: {})",
                            coordinates, existing.getPomPath());
                    continue;
                }

                if ("pom".equals(model.getPackaging())) {
                    analyzePomDependencies(searchingDir, pomPath, model, result);
                } else {
                    Path jarPath = findJarForPom(pomPath, model);
                    if (jarPath != null && Files.exists(jarPath)) {
                        ArtifactData artifactData = artifactFactory.data(coordinates, model, pomPath, jarPath);
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

    private void analyzePomDependencies(String searchingDir, Path pomPath, Model model,
                                        HashMap<String, Path> result) {
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
                        ArtifactCoordinates depCoordinates = extractCoordinates(dependencyModel);

                        if (!artifactCache.contains(depCoordinates)) {
                            analyzePomDependencies(searchingDir, dependencyPomPath, dependencyModel,
                                    result);
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
                    ArtifactCoordinates coordinates = artifactFactory.coordinates(
                            dependency.getGroupId(), dependencyArtifactId, dependencyVersion);

                    if (!artifactCache.contains(coordinates)) {
                        try {
                            Model jarModel = createModelForDependency(dependency);
                            ArtifactData artifactData = artifactFactory.data(coordinates, jarModel, pomPath, jarPath);
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

    private ArtifactCoordinates extractCoordinates(Model model) {
        String groupId = model.getGroupId();
        String artifactId = model.getArtifactId();
        String version = model.getVersion();

        if (groupId == null && model.getParent() != null) {
            groupId = model.getParent().getGroupId();
        }
        if (version == null && model.getParent() != null) {
            version = model.getParent().getVersion();
        }

        return artifactFactory.coordinates(groupId, artifactId, version);
    }

    private Model createModelForDependency(Dependency dependency) {
        Model model = new Model();
        model.setGroupId(dependency.getGroupId());
        model.setArtifactId(dependency.getArtifactId());
        model.setVersion(dependency.getVersion());
        return model;
    }

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

    private Model readModel(Path pomPath) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try (FileInputStream fis = new FileInputStream(pomPath.toFile())) {
            return reader.read(fis);
        }
    }
}
