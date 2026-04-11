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

package org.altlinux.xgradle.impl.indexing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.altlinux.xgradle.interfaces.collectors.PomFilesCollector;
import org.altlinux.xgradle.interfaces.indexing.PomIndex;
import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.gradle.api.logging.Logger;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.IntStream;
/**
 * Index for POM.
 * Implements {@link PomIndex}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

@Singleton
final class DefaultPomIndex implements PomIndex {

    private final PomFilesCollector pomFilesCollector;
    private final PomParser pomParser;
    private final Logger logger;

    private volatile Map<String, MavenCoordinate> byGa = new LinkedHashMap<>();
    private volatile Map<String, List<MavenCoordinate>> byGroup = new LinkedHashMap<>();

    @Inject
    DefaultPomIndex(PomFilesCollector pomFilesCollector, PomParser pomParser, Logger logger) {
        this.pomFilesCollector = pomFilesCollector;
        this.pomParser = pomParser;
        this.logger = logger;
    }

    @Override
    public synchronized void build(Path rootDirectory) {
        List<Path> files = pomFilesCollector.collect(rootDirectory);
        build(files);
    }

    @Override
    public synchronized void build(List<Path> pomFiles) {
        Map<String, MavenCoordinate> newByGa = new LinkedHashMap<>();
        Map<String, List<MavenCoordinate>> newByGroup = new LinkedHashMap<>();

        pomFiles.stream()
                .map(pomParser::parsePom)
                .filter(Objects::nonNull)
                .forEach(coordinate -> {
                    String groupArtifactKey = coordinate.getGroupId() + ":" + coordinate.getArtifactId();

                    MavenCoordinate previousCoordinate = newByGa.get(groupArtifactKey);
                    if (previousCoordinate == null
                            || isNewer(coordinate.getVersion(), previousCoordinate.getVersion())) {
                        newByGa.put(groupArtifactKey, coordinate);
                    }

                    newByGroup.computeIfAbsent(coordinate.getGroupId(), groupId -> new ArrayList<>())
                            .add(coordinate);
                });

        newByGroup.values().forEach(coordinates -> coordinates.sort(
                Comparator.comparing(MavenCoordinate::getArtifactId)
                        .thenComparing(MavenCoordinate::getVersion, this::compareVersions)));

        byGa = newByGa;
        byGroup = newByGroup;

        logger.lifecycle("POM index built: {} artifacts, {} groups", byGa.size(), byGroup.size());
    }

    @Override
    public Optional<MavenCoordinate> find(String groupId, String artifactId) {
        return Optional.ofNullable(byGa.get(groupId + ":" + artifactId));
    }

    @Override
    public List<MavenCoordinate> findAllForGroup(String groupId) {
        return byGroup.getOrDefault(groupId, List.of());
    }

    @Override
    public Map<String, MavenCoordinate> snapshot() {
        return Collections.unmodifiableMap(byGa);
    }

    private boolean isNewer(String leftVersion, String rightVersion) {
        return compareVersions(leftVersion, rightVersion) > 0;
    }

    private int compareVersions(String leftVersion, String rightVersion) {
        if (leftVersion == null && rightVersion == null) {
            return 0;
        }
        if (leftVersion == null) {
            return -1;
        }
        if (rightVersion == null) {
            return 1;
        }

        String[] leftParts = leftVersion.split("[.-]");
        String[] rightParts = rightVersion.split("[.-]");
        int maxLength = Math.max(leftParts.length, rightParts.length);

        return IntStream.range(0, maxLength)
                .map(index -> {
                    String leftPart = index < leftParts.length ? leftParts[index] : "0";
                    String rightPart = index < rightParts.length ? rightParts[index] : "0";
                    return comparePart(leftPart, rightPart);
                })
                .filter(partComparison -> partComparison != 0)
                .findFirst()
                .orElse(0);
    }

    private int comparePart(String leftPart, String rightPart) {
        boolean leftIsNumeric = leftPart.chars().allMatch(Character::isDigit);
        boolean rightIsNumeric = rightPart.chars().allMatch(Character::isDigit);

        if (leftIsNumeric && rightIsNumeric) {
            int leftNumericPart = Integer.parseInt(leftPart);
            int rightNumericPart = Integer.parseInt(rightPart);
            return Integer.compare(leftNumericPart, rightNumericPart);
        }

        if (leftIsNumeric) {
            return 1;
        }
        if (rightIsNumeric) {
            return -1;
        }

        return leftPart.compareTo(rightPart);
    }
}
