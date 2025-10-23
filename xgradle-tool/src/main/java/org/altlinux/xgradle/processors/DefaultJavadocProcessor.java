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
package org.altlinux.xgradle.processors;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.altlinux.xgradle.ToolConfig;
import org.altlinux.xgradle.api.parsers.PomParser;
import org.altlinux.xgradle.api.processors.PomProcessor;
import org.altlinux.xgradle.api.services.PomService;
import org.altlinux.xgradle.model.DefaultArtifactCoordinates;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Handles processing of Javadoc JAR files and filters duplicates based on groupId:artifactId.
 * Prevents multiple Javadoc artifacts for the same library.
 *
 * @author Ivan Khanas
 */
@Singleton
public class DefaultJavadocProcessor implements PomProcessor<HashMap<String, Path>> {
    private static final Logger logger = LoggerFactory.getLogger("XGradleLogger");
    private final PomParser<HashMap<String, Path>> javadocParser;
    private final PomService pomService;
    private final ToolConfig toolConfig;

    /**
     * Constructs a new DefaultJavadocProcessor with required dependencies.
     *
     * @param javadocParser parser for Javadoc artifacts
     * @param pomService service for POM processing operations
     * @param toolConfig configuration for the tool
     */
    @Inject
    public DefaultJavadocProcessor(
            @Named("Javadoc") PomParser<HashMap<String, Path>> javadocParser,
            PomService pomService,
            ToolConfig toolConfig
    ) {
        this.javadocParser = javadocParser;
        this.pomService = pomService;
        this.toolConfig = toolConfig;
    }

    /**
     * Processes Javadoc artifacts from the specified directory.
     * Applies duplicate filtering, artifact exclusion, and snapshot filtering.
     *
     * @param searchingDir the directory to search for Javadoc artifacts
     * @param artifactName optional list of artifact names to filter by
     * @return map of POM file paths to corresponding Javadoc JAR file paths
     */
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

    /**
     * Filters duplicate artifacts based on groupId:artifactId coordinates.
     * Ensures only one Javadoc artifact per unique library coordinates.
     *
     * @param artifacts map of artifact paths to filter
     * @return filtered map with unique artifacts by coordinates
     */
    private HashMap<String, Path> filterDuplicateArtifacts(HashMap<String, Path> artifacts) {
        HashMap<String, Path> filteredArtifacts = new HashMap<>();
        Set<String> processedCoordinates = new HashSet<>();

        for (HashMap.Entry<String, Path> entry : artifacts.entrySet()) {
            Path pomPath = Path.of(entry.getKey());
            Path javadocPath = entry.getValue();

            try {
                DefaultArtifactCoordinates coordinates = extractCoordinatesFromPom(pomPath);
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

    /**
     * Extracts artifact coordinates from POM file.
     * Reads groupId, artifactId, and version from POM model.
     *
     * @param pomPath path to the POM file
     * @return artifact coordinates or null if extraction fails
     * @throws IOException if an I/O error occurs during file reading
     * @throws XmlPullParserException if the POM file cannot be parsed
     */
    private DefaultArtifactCoordinates extractCoordinatesFromPom(Path pomPath) throws IOException, XmlPullParserException {
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

            return new DefaultArtifactCoordinates(groupId, artifactId, version != null ? version : "unknown");
        }
    }
}