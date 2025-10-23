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

import org.altlinux.xgradle.api.containers.PomContainer;
import org.altlinux.xgradle.api.parsers.PomParser;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.List;

/**
 * Concurrent parser for Javadoc JAR files.
 * Handles parsing of POM files to find corresponding Javadoc JAR files.
 * Uses concurrent processing for improved performance.
 *
 * @author Ivan Khanas
 */
@Singleton
public class ConcurrentJavadocParser implements PomParser<HashMap<String, Path>> {
    private static final Logger logger = LoggerFactory.getLogger("XGradleLogger");
    private final PomContainer pomContainer;

    /**
     * Constructs a new ConcurrentJavadocParser with required dependencies.
     *
     * @param pomContainer container for POM file management
     */
    @Inject
    public ConcurrentJavadocParser(PomContainer pomContainer) {
        this.pomContainer = pomContainer;
    }

    /**
     * Retrieves Javadoc artifact mappings from the specified directory.
     * For each POM file, finds the corresponding Javadoc JAR file.
     *
     * @param searchingDir the directory to search for POM files
     * @param artifactNames optional list of artifact names to filter by
     * @return map of POM file paths to corresponding Javadoc JAR file paths
     */
    @Override
    public HashMap<String, Path> getArtifactCoords(String searchingDir, Optional<List<String>> artifactNames) {
        Collection<Path> pomPaths;

        if (artifactNames.isPresent()) {
            pomPaths = pomContainer.getSelectedPoms(searchingDir, artifactNames.get());
        } else {
            pomPaths = pomContainer.getAllPoms(searchingDir);
        }

        HashMap<String, Path> javadocMap = new HashMap<>();

        for (Path pomPath : pomPaths) {
            try {
                Path javadocPath = findJavadocForPom(pomPath);
                if (javadocPath != null && Files.exists(javadocPath)) {
                    javadocMap.put(pomPath.toString(), javadocPath);
                }
            } catch (Exception e) {
                logger.error("Error processing POM file for Javadoc: {}", pomPath, e);
            }
        }

        logger.info("Found {} Javadoc JAR files from {} POM files", javadocMap.size(), pomPaths.size());
        return javadocMap;
    }

    /**
     * Finds the corresponding Javadoc JAR file for a POM file.
     *
     * @param pomPath path to the POM file
     * @return path to the corresponding Javadoc JAR file, or null if not found
     * @throws IOException if an I/O error occurs during file reading
     * @throws XmlPullParserException if the POM file cannot be parsed
     */
    private Path findJavadocForPom(Path pomPath) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try (FileInputStream fis = new FileInputStream(pomPath.toFile())) {
            Model model = reader.read(fis);
            String artifactId = model.getArtifactId();
            String version = model.getVersion();

            if (version == null && model.getParent() != null) {
                version = model.getParent().getVersion();
            }

            if (artifactId == null || version == null) {
                logger.warn("Could not determine artifactId or version for POM: {}", pomPath);
                return null;
            }

            String javadocFileName = artifactId + "-" + version + "-javadoc.jar";
            return pomPath.getParent().resolve(javadocFileName);
        }
    }
}