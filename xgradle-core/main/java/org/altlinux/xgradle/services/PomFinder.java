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
package org.altlinux.xgradle.services;

import org.altlinux.xgradle.api.PomParser;
import org.altlinux.xgradle.extensions.SystemDepsExtension;
import org.altlinux.xgradle.model.MavenCoordinate;

import org.gradle.api.logging.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service for locating and processing POM files.
 * Handles searching for POM files based on artifact coordinates and
 * validating their contents.
 *
 * @author Ivan Khanas
 */
public class PomFinder {

    private static final List<Path> POM_DIRS = List.of(Paths.get(SystemDepsExtension.getPomsPath()));
    private static final Pattern VERSION_PATTERN = Pattern.compile("^[0-9][\\w.-]*$");

    private final PomParser pomParser;

    /**
     * Constructs a PomFinder object with the specified POM parser.
     *
     * @param pomParser the parser implementation to use for
     *                  reading POM file contents
     */
    public PomFinder(PomParser pomParser) {
        this.pomParser = pomParser;
    }

    /**
     * Getter for PomParser object.
     *
     * @return PomParser object.
     */
    public PomParser getPomParser() {
        return pomParser;
    }

    /**
     * Locates a POM file for the specified artifact by searching
     * predefined directories and validates its contents.
     *
     * <p>The search uses generated filename variants based on the
     * groupId and artifactId, and checks candidate files matching
     * these patterns. Each candidate POM is parsed and validated
     * to ensure it matches the requested artifact coordinates.
     *
     * @param groupId Maven group ID of the artifact
     * @param artifactId Maven artifact ID of the artifact
     * @param logger logger instance for reporting progress and errors
     *
     * @return valid MavenCoordinate if matching POM is found,
     *         or null if no suitable POM is located
     */
    public MavenCoordinate findPomForArtifact(String groupId, String artifactId, Logger logger) {
        Set<String> nameVariants = generateNameVariants(groupId, artifactId);

        for (Path dir : POM_DIRS) {
            if (!Files.isDirectory(dir)) continue;

            for (String variant : nameVariants) {
                try (var stream = Files.walk(dir, 3)) {
                    Optional<MavenCoordinate> result = stream
                            .filter(Files::isRegularFile)
                            .filter(path -> path.toString().endsWith(".pom"))
                            .filter(path -> matchesPattern(path, variant, artifactId))
                            .map(pomPath -> pomParser.parsePom(pomPath, logger))
                            .filter(coord -> isValidCoordinate(coord, groupId, artifactId, logger))
                            .findFirst();

                    if (result.isPresent()) {
                        return result.get();
                    }
                } catch (Exception e) {
                    logger.lifecycle("POM search error: {}", e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * Determines if a file path matches expected naming patterns
     * for an artifact POM.
     *
     * <p>Patterns include:
     * <ul>
     *   <li>Exact match to the base variant name</li>
     *   <li>Variant name followed by a version suffix</li>
     *   <li>Variant name followed by version and artifact ID</li>
     * </ul>
     *
     * @param path filesystem path to candidate POM file
     * @param variant base filename variant to match
     * @param artifactId expected artifact ID for additional validation
     *
     * @return true if filename matches any valid pattern, false otherwise
     */
    private boolean matchesPattern(Path path, String variant, String artifactId) {
        String fileName = path.getFileName().toString();
        String baseName = fileName.substring(0, fileName.length() - 4);
        if (baseName.equals(variant)) return true;
        if (baseName.startsWith(variant + "-")) {
            String suffix = baseName.substring(variant.length() + 1);
            return isVersionString(suffix) || isVersionedArtifact(suffix, artifactId);
        }
        return false;
    }

    /**
     * Validates that a parsed Maven coordinate matches expected values
     * and is structurally valid.
     *
     * @param coord parsed Maven coordinate to validate
     * @param groupId expected group ID
     * @param artifactId expected artifact ID
     * @param logger logger for reporting validation failures
     *
     * @return true if coordinate is valid and matches expectations,
     *         false otherwise
     */
    private boolean isValidCoordinate(MavenCoordinate coord, String groupId,
                                      String artifactId, Logger logger) {
        if (coord == null) return false;
        if (!coord.isValid()) {
            logger.lifecycle("Invalid coordinate in POM");
            return false;
        }
        if (!groupId.equals(coord.getGroupId())) {
            logger.lifecycle("Group ID mismatch: expected {} but found {}",
                    groupId, coord.getGroupId());
            return false;
        }
        if (!artifactId.equals(coord.getArtifactId())) {
            logger.lifecycle("Artifact ID mismatch: expected {} but found {}",
                    artifactId, coord.getArtifactId());
            return false;
        }
        return true;
    }

    /**
     * Checks if a string represents a valid version identifier.
     *
     * @param str string to check for version pattern compliance
     *
     * @return true if string matches version pattern, false otherwise
     */
    private boolean isVersionString(String str) {
        return str != null && !str.isEmpty() && VERSION_PATTERN.matcher(str).matches();
    }

    /**
     * Determines if a suffix follows the pattern: [version]-[artifactId]
     * where the version part is a valid version string.
     *
     * @param suffix string to analyze
     * @param artifactId expected artifact ID for validation
     *
     * @return true if suffix matches expected pattern, false otherwise
     */
    private boolean isVersionedArtifact(String suffix, String artifactId) {
        return suffix.endsWith(artifactId) &&
                suffix.length() > artifactId.length() &&
                isVersionString(suffix.substring(0, suffix.length() - artifactId.length() - 1));
    }

    /**
     * Generates possible filename variants for an artifact based on
     * its group ID and artifact ID.
     *
     * <p>Variants include:
     * <ul>
     *   <li>Basic artifact ID</li>
     *   <li>Group suffix + artifact ID (e.g., "apache-commons" for org.apache.commons)</li>
     *   <li>First subgroup + artifact ID (e.g., "apache-commons" but only using "apache")</li>
     * </ul>
     *
     * @param groupId dependency groupId
     * @param artifactId dependency artifactId
     *
     * @return set of generated filename variants
     */
    private Set<String> generateNameVariants(String groupId, String artifactId) {
        Set<String> variants = new HashSet<>();
        variants.add(artifactId);
        String[] groupParts = groupId.split("\\.");

        if (groupParts.length > 1) {
            String groupSuffix = String.join("-", Arrays.copyOfRange(groupParts, 1, groupParts.length));
            variants.add(groupSuffix + "-" + artifactId);

            if (groupParts.length > 2) {
                variants.add(groupParts[1] + "-" + artifactId);
            }
        }
        return variants;
    }

    /**
     * Finds all POM files belonging to a specific group by scanning
     * predefined directories.
     *
     * @param groupId groupId to search for
     * @param logger logger instance for reporting progress and errors
     *
     * @return list of valid MavenCoordinate objects found for the group
     */
    public ArrayList<MavenCoordinate> findAllPomsForGroup(String groupId, Logger logger) {
        ArrayList<MavenCoordinate> candidates = new ArrayList<>();
        for (Path dir : POM_DIRS) {
            if (!Files.isDirectory(dir)) continue;

            try (var stream = Files.walk(dir, 3)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".pom"))
                        .forEach(pomPath -> {
                            MavenCoordinate coord = pomParser.parsePom(pomPath, logger);
                            if (coord != null && coord.isValid() && groupId.equals(coord.getGroupId())) {
                                candidates.add(coord);
                            }
                        });
            } catch (Exception e) {
                logger.lifecycle("Group POM scan error: {}", e.getMessage());
            }
        }
        return candidates;
    }
}