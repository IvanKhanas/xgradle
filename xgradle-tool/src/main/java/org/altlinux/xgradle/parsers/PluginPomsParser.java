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

public class PluginPomsParser implements PomParser {
    private static final Logger logger = LoggerFactory.getLogger("XGradleLogger");

    private final PomContainer pomContainer;

    @Inject
    public PluginPomsParser(PomContainer pomContainer) {
        this.pomContainer = pomContainer;
    }

    @Override
    public PluginPomsParser parsePoms() {
        return this;
    }

    @Override
    public HashMap<String, Path> getArtifactCoords(String searchingDir, Optional<String> artifactName) {
        if (!artifactName.isPresent()) {
            return new HashMap<>();
        }

        HashMap<String, Path> result = new HashMap<>();
        String artifactNameValue = artifactName.get();

        Collection<Path> allPomPaths = pomContainer.getAllPomPaths(searchingDir);

        List<Path> filteredPomPaths = allPomPaths.stream()
                .filter(path -> path.getFileName().toString().startsWith(artifactNameValue))
                .collect(Collectors.toList());

        if (filteredPomPaths.isEmpty()) {
            logger.warn("No POM files found for artifact: {}", artifactNameValue);
            return result;
        }

        for (Path pomPath : filteredPomPaths) {
            try {
                Model model = readModel(pomPath);

                if ("pom".equals(model.getPackaging())) {
                    analyzePomDependencies(searchingDir, pomPath, model, result);
                } else {
                    Path jarPath = findJarForPom(pomPath, model);
                    if (jarPath != null && Files.exists(jarPath)) {
                        result.put(pomPath.toString(), jarPath);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to process POM file: {}", pomPath, e);
            }
        }

        return result;
    }

    private void analyzePomDependencies(String searchingDir, Path pomPath, Model model, HashMap<String, Path> result) {
        if (model.getDependencies() == null) {
            return;
        }

        Collection<Path> allPomPaths = pomContainer.getAllPomPaths(searchingDir);
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
                        analyzePomDependencies(searchingDir, dependencyPomPath, dependencyModel, result);
                    } catch (Exception e) {
                        logger.error("Failed to analyze dependency POM: {}", dependencyPomPath, e);
                    }
                }
            } else if (!"pom".equals(dependencyType)) {
                String jarFileName = dependencyArtifactId + "-" + dependencyVersion + ".jar";
                Path jarPath = pomPath.getParent().resolve(jarFileName);

                if (Files.exists(jarPath)) {
                    result.put(pomPath.toString(), jarPath);
                }
            }
        }
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