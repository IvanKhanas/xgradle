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
package org.altlinux.xgradle.impl.resolvers;

import org.altlinux.xgradle.interfaces.resolvers.PluginPomChainResolver;
import org.altlinux.xgradle.interfaces.resolvers.PluginPomChainResult;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.containers.PomContainer;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import org.slf4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves full POM chain for Gradle plugins (parent + pom dependencies).
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
public final class DefaultPluginPomChainResolver implements PluginPomChainResolver {

    private final PomContainer pomContainer;
    private final Logger logger;

    @Inject
    public DefaultPluginPomChainResolver(PomContainer pomContainer, Logger logger) {
        this.pomContainer = pomContainer;
        this.logger = logger;
    }

    public PluginPomChainResult resolve(
            String searchingDirectory,
            List<String> artifactNames,
            Map<String, Path> artifactsMap
    ) {
        Map<Path, Model> pomModels = new HashMap<>();
        Map<String, Path> pomByCoords = new HashMap<>();
        indexPoms(searchingDirectory, pomModels, pomByCoords);

        Set<Path> pomPaths = resolvePomChain(
                searchingDirectory,
                artifactNames,
                artifactsMap,
                pomModels,
                pomByCoords
        );

        return new PluginPomChainResult(pomModels, pomPaths);
    }

    private void indexPoms(String searchingDirectory,
                           Map<Path, Model> pomModels,
                           Map<String, Path> pomByCoords) {
        Set<Path> allPomPaths = pomContainer.getAllPoms(searchingDirectory);
        for (Path pomPath : allPomPaths) {
            try {
                Model model = readPomModel(pomPath);
                pomModels.put(pomPath, model);
                String key = coordinatesKey(model);
                if (key != null) {
                    pomByCoords.putIfAbsent(key, pomPath);
                }
            } catch (IOException | XmlPullParserException e) {
                logger.error("Failed to read POM file: {}", pomPath, e);
            }
        }
    }

    private Set<Path> resolvePomChain(
            String searchingDirectory,
            List<String> artifactNames,
            Map<String, Path> artifactsMap,
            Map<Path, Model> pomModels,
            Map<String, Path> pomByCoords
    ) {
        LinkedHashSet<Path> collected = new LinkedHashSet<>();

        if (artifactsMap != null) {
            for (String pomPathStr : artifactsMap.keySet()) {
                collected.add(Paths.get(pomPathStr));
            }
        }

        if (artifactNames != null && !artifactNames.isEmpty()) {
            collected.addAll(pomContainer.getSelectedPoms(searchingDirectory, artifactNames));
        }

        Deque<Path> queue = new ArrayDeque<>(collected);
        Set<Path> visited = new HashSet<>(collected);

        while (!queue.isEmpty()) {
            Path current = queue.removeFirst();
            Model model = pomModels.get(current);
            if (model == null) {
                continue;
            }

            Parent parent = model.getParent();
            Path parentPath = resolvePom(parent, pomByCoords);
            if (parentPath != null && visited.add(parentPath)) {
                queue.add(parentPath);
            }

            if (model.getDependencies() != null) {
                for (Dependency dependency : model.getDependencies()) {
                    Path depPath = resolvePom(dependency, pomByCoords);
                    if (depPath != null && visited.add(depPath)) {
                        queue.add(depPath);
                    }
                }
            }
        }

        collected.clear();
        collected.addAll(visited);
        return collected;
    }

    private Path resolvePom(Parent parent, Map<String, Path> pomByCoords) {
        if (parent == null) {
            return null;
        }
        return pomByCoords.get(coordinatesKey(parent.getGroupId(), parent.getArtifactId(), parent.getVersion()));
    }

    private Path resolvePom(Dependency dependency, Map<String, Path> pomByCoords) {
        if (dependency == null) {
            return null;
        }
        return pomByCoords.get(
                coordinatesKey(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()));
    }

    private String coordinatesKey(Model model) {
        if (model == null) {
            return null;
        }
        String groupId = model.getGroupId();
        String artifactId = model.getArtifactId();
        String version = model.getVersion();

        if (groupId == null && model.getParent() != null) {
            groupId = model.getParent().getGroupId();
        }
        if (version == null && model.getParent() != null) {
            version = model.getParent().getVersion();
        }
        return coordinatesKey(groupId, artifactId, version);
    }

    private String coordinatesKey(String groupId, String artifactId, String version) {
        if (groupId == null || artifactId == null || version == null) {
            return null;
        }
        return groupId + ":" + artifactId + ":" + version;
    }

    private Model readPomModel(Path pomPath) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try (FileInputStream fis = new FileInputStream(pomPath.toFile())) {
            return reader.read(fis);
        }
    }
}
