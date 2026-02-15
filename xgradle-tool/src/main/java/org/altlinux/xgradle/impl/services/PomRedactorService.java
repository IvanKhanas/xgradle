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

package org.altlinux.xgradle.impl.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.containers.PomContainer;
import org.altlinux.xgradle.interfaces.redactors.PomRedactor;
import org.altlinux.xgradle.interfaces.services.PomService;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for POM file processing operations.
 * Implements {@link PomService}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class PomRedactorService implements PomService {
    private final PomRedactor pomRedactor;
    private final PomContainer pomContainer;

    @Inject
    PomRedactorService(PomRedactor pomRedactor, PomContainer pomContainer) {
        this.pomRedactor = pomRedactor;
        this.pomContainer = pomContainer;

    }

    @Override
    public void addDependency(Path pomOrDir, String coords, boolean recursive) {
        targets(pomOrDir, recursive).forEach(p -> pomRedactor.addDependency(p, coords));
    }

    @Override
    public void removeDependency(Path pomOrDir, String coords, boolean recursive) {
        targets(pomOrDir, recursive).forEach(p -> pomRedactor.removeDependency(p, coords));
    }

    @Override
    public void changeDependency(Path pomOrDir, String sourceCoords, String targetCoords, boolean recursive) {
        targets(pomOrDir, recursive).forEach(p -> pomRedactor.changeDependency(p, sourceCoords, targetCoords));
    }

    private Stream<Path> targets(Path pomOrDir, boolean recursive) {
        return recursive
                ? pomContainer.getAllPoms(pomOrDir.toString()).stream()
                : Stream.of(pomOrDir);
    }

    @Override
    public HashMap<String, Path> excludeArtifacts(List<String> excludedArtifacts, HashMap<String, Path> artifactCoordinatesMap) {
        if (excludedArtifacts != null && !excludedArtifacts.isEmpty()) {
            return artifactCoordinatesMap.entrySet().stream()
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
        } else {
            return artifactCoordinatesMap;
        }
    }

    @Override
    public Set<Path> excludeArtifacts(List<String> excludedArtifacts, Set<Path> artifactCoords) {
        if (excludedArtifacts != null && !excludedArtifacts.isEmpty()) {
            return artifactCoords.stream()
                    .filter(path -> excludedArtifacts.stream()
                            .noneMatch(path.getFileName().toString()::startsWith))
                    .collect(Collectors.toSet());
        }
        return artifactCoords;
    }

    @Override
    public void removeParentBlocks(HashMap<String, Path> artifacts, List<String> removeParentPoms) {
        if (removeParentPoms == null || removeParentPoms.isEmpty()) {
            return;
        }
        artifacts.forEach((pom, jar) -> {
            Path pomPath = Path.of(pom);
            String filename = pomPath.getFileName().toString();

            boolean shouldRemove = removeParentPoms.contains("all") ||
                    removeParentPoms.stream().anyMatch(filename::startsWith);

            if (shouldRemove) {
                pomRedactor.removeParent(pomPath);
            }
        });
    }

    @Override
    public void removeParentBlocks(Set<Path> bomFiles, List<String> removeParentPoms) {
        if (removeParentPoms == null || removeParentPoms.isEmpty()) {
            return;
        }

        bomFiles.forEach(bomFile -> {
            String fileName = bomFile.getFileName().toString();
            boolean shouldRemove = removeParentPoms.contains("all") ||
                    removeParentPoms.stream().anyMatch(fileName::startsWith);

            if (shouldRemove) {
                pomRedactor.removeParent(bomFile);
            }
        });
    }

    @Override
    public Set<Path> excludeSnapshots(Set<Path> pomFiles) {
        return pomFiles.stream()
                .filter(pomPath -> !isSnapshotPom(pomPath))
                .collect(Collectors.toSet());
    }

    @Override
    public HashMap<String, Path> excludeSnapshots(HashMap<String, Path> artifactsMap) {
        return artifactsMap.entrySet().stream().
                filter (entry -> {
                    Path pomPath = Path.of(entry.getKey());

                    return !isSnapshotPom(pomPath);
                }).collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        HashMap::new
                ));
    }

    private boolean isSnapshotPom(Path pomPath) {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model;

        if (pomPath.getFileName().toString().toLowerCase().contains("snapshot")) {
            return true;
        }

        try {
            File pomFile = new File(String.valueOf(pomPath));
            model = reader.read(new FileReader(pomFile));

            if (model.getVersion() != null && model.getVersion().toLowerCase().contains("snapshot")) {
                return true;
            }

        } catch (XmlPullParserException | IOException e) {
            throw new RuntimeException("Unable to initialize POM model for:" + pomPath, e);
        }
        return false;
    }
}
