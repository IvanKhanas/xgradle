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

import org.altlinux.xgradle.interfaces.containers.PomContainer;
import org.altlinux.xgradle.interfaces.parsers.PomParser;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.slf4j.Logger;

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
 * Implements {@link PomParser<HashMap<String} and {@link Path>>}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class ConcurrentJavadocParser implements PomParser<HashMap<String, Path>> {

    private final PomContainer pomContainer;
    private final Logger logger;

    @Inject
    ConcurrentJavadocParser(PomContainer pomContainer, Logger logger) {
        this.pomContainer = pomContainer;
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
