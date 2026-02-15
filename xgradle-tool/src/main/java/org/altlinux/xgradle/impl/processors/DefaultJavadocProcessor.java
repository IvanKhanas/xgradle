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
package org.altlinux.xgradle.impl.processors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.model.ArtifactCoordinates;
import org.altlinux.xgradle.interfaces.model.ArtifactFactory;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Javadoc;
import org.altlinux.xgradle.impl.config.ToolConfig;
import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.altlinux.xgradle.interfaces.processors.PomProcessor;
import org.altlinux.xgradle.interfaces.services.PomService;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.slf4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.List;

/**
 * Processor for Javadoc artifacts with duplicate filtering.
 * Implements {@link PomProcessor<HashMap<String} and {@link Path>>}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultJavadocProcessor implements PomProcessor<HashMap<String, Path>> {

    private final ArtifactFactory artifactFactory;
    private final PomParser<HashMap<String, Path>> javadocParser;
    private final PomService pomService;
    private final ToolConfig toolConfig;
    private final Logger logger;

    @Inject
    DefaultJavadocProcessor(
            ArtifactFactory artifactFactory,
            @Javadoc PomParser<HashMap<String, Path>> javadocParser,
            PomService pomService,
            ToolConfig toolConfig,
            Logger logger
    ) {
        this.artifactFactory =artifactFactory;
        this.javadocParser = javadocParser;
        this.pomService = pomService;
        this.toolConfig = toolConfig;
        this.logger = logger;
    }

    @Override
    public HashMap<String, Path> pomsFromDirectory(String searchingDir, Optional<List<String>> artifactName) {
        HashMap<String, Path> artifacts = javadocParser.getArtifactCoords(searchingDir, artifactName);

        artifacts = filterDuplicateArtifacts(artifacts);
        artifacts = pomService.excludeArtifacts(toolConfig.getExcludedArtifacts(), artifacts);

        if (!toolConfig.isAllowSnapshots()) {
            artifacts = pomService.excludeSnapshots(artifacts);
        }

        return artifacts;
    }

    private HashMap<String, Path> filterDuplicateArtifacts(HashMap<String, Path> artifacts) {
        HashMap<String, Path> filteredArtifacts = new HashMap<>();
        Set<String> processedCoordinates = new HashSet<>();

        for (HashMap.Entry<String, Path> entry : artifacts.entrySet()) {
            Path pomPath = Path.of(entry.getKey());
            Path javadocPath = entry.getValue();

            try {
                ArtifactCoordinates coordinates = extractCoordinatesFromPom(pomPath);
                if (coordinates != null) {
                    String coordKey = coordinates.getGroupId() + ":" + coordinates.getArtifactId();

                    if (!processedCoordinates.contains(coordKey)) {
                        processedCoordinates.add(coordKey);
                        filteredArtifacts.put(pomPath.toString(), javadocPath);
                        logger.debug("Added Javadoc artifact: {} for coordinates: {}",
                                javadocPath.getFileName(), coordKey);
                    } else {
                        logger.debug("Filtered out duplicate Javadoc artifact: {} for coordinates: {}",
                                javadocPath.getFileName(), coordKey);
                    }
                } else {
                    filteredArtifacts.put(pomPath.toString(), javadocPath);
                }
            } catch (Exception e) {
                logger.warn("Failed to extract coordinates from POM: {}, adding artifact anyway", pomPath);
                filteredArtifacts.put(pomPath.toString(), javadocPath);
            }
        }

        logger.debug("Filtered {} Javadoc artifacts to {} unique artifacts by groupId:artifactId",
                artifacts.size(), filteredArtifacts.size());
        return filteredArtifacts;
    }

    private ArtifactCoordinates extractCoordinatesFromPom(Path pomPath) throws IOException, XmlPullParserException {
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

            if (groupId == null || artifactId == null) {
                logger.warn("Could not determine groupId or artifactId for POM: {}", pomPath);
                return null;
            }

            return artifactFactory.coordinates(groupId, artifactId, version != null ? version : "unknown");
        }
    }
}
