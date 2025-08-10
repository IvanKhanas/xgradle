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
package org.altlinux.gradlePlugin.services;

import org.altlinux.gradlePlugin.api.ArtifactVerifier;
import org.altlinux.gradlePlugin.extensions.SystemDepsExtension;
import org.altlinux.gradlePlugin.model.MavenCoordinate;

import org.gradle.api.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Implementation of {@link ArtifactVerifier} that checks for the physical
 * presence of artifact files in the local filesystem.
 *
 * @author Ivan Khanas
 */
public class FileSystemArtifactVerifier implements ArtifactVerifier {

    /**
     * Verifies if an artifact file exists in the local repository structure.
     *
     * <p>This method checks for the presence of JAR files matching common
     * Maven naming patterns. It supports multiple naming conventions:
     * <ul>
     *   <li>Simple artifact name (artifactId.jar)</li>
     *   <li>Versioned artifact name (artifactId-version.jar)</li>
     *   <li>Recursive search for artifacts in subdirectories (maxDepth 3)</li>
     * </ul>
     *
     * <p>Special case: BOM artifacts (packaging type 'pom') are considered
     * to always exist as their presence is verified separately.
     *
     * @param coord coordinates of the artifact to verify
     * @param logger logger for diagnostic messages
     *
     * @return true if the artifact file is found or is a BOM artifact,
     *         false otherwise
     */
    @Override
    public boolean verifyArtifactExists(MavenCoordinate coord, Logger logger) {
        if (coord == null || !coord.isValid()) {
            return false;
        }

        if ("pom".equals(coord.getPackaging())) {
            return true;
        }

        Path basePath = Paths.get(SystemDepsExtension.getJarsPath());
        String basePattern = coord.getArtifactId();
        String versionedPattern = coord.getArtifactId() + "-" + coord.getVersion();

        return checkArtifactExists(basePath, basePattern + ".jar") ||
                checkArtifactExists(basePath, versionedPattern + ".jar") ||
                checkRecursively(basePath, basePattern, coord.getVersion());
    }

    /**
     * Checks if the artifact exists in directory.
     *
     * @param baseDir directory to search
     * @param fileName name of a file exact filename to look for
     *
     * @return true if artifact exists and false otherwise
     */
    private boolean checkArtifactExists(Path baseDir, String fileName) {
        Path filePath = baseDir.resolve(fileName);
        if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
            return true;
        }
        return false;
    }

    /**
     * Recursively searches for artifact files matching naming patterns.
     *
     * <p>Searches up to 3 directory levels deep for files matching:
     * <ul>
     *   <li>artifactId.jar</li>
     *   <li>artifactId-version.jar</li>
     *   <li>artifactId-*.jar where * contains a digit (indicating a version)</li>
     * </ul>
     *
     * @param baseDir root directory to start search from
     * @param artifactId expected artifact ID
     * @param version expected version (optional)
     *
     * @return true if any matching file is found, false otherwise
     */
    private boolean checkRecursively(Path baseDir, String artifactId, String version) {
        try (var pathStream = Files.walk(baseDir, 3)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .anyMatch(path -> {
                        String fileName = path.getFileName().toString();
                        return matchesArtifactPattern(fileName, artifactId, version);
                    });
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Determines if a filename matches artifact naming patterns.
     *
     * <p>Supported patterns:
     * <ul>
     *   <li>Exact artifact ID match (artifactId.jar)</li>
     *   <li>Versioned artifact (artifactId-version.jar)</li>
     *   <li>Artifact with version suffix (artifactId-1.2.3.jar)</li>
     * </ul>
     *
     * @param fileName filename to check
     * @param artifactId expected artifact ID
     * @param version expected version (optional)
     * @return true if filename matches any valid artifact pattern, false otherwise
     */
    private boolean matchesArtifactPattern(String fileName, String artifactId, String version) {
        if (!fileName.endsWith(".jar")) return false;

        String baseName = fileName.substring(0, fileName.length() - 4);

        if (baseName.equals(artifactId)) {
            return true;
        }

        if (baseName.startsWith(artifactId + "-")) {
            String suffix = baseName.substring(artifactId.length() + 1);

            if (suffix.equals(version)) {
                return true;
            }

            if (isVersionString(suffix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Heuristically determines if a string represents a version identifier.
     *
     * <p>This implementation considers any string containing at least one digit
     * as a potential version string. This is a pragmatic approach since version
     * formats vary widely in repositories.
     *
     * @param str string to evaluate
     *
     * @return true if the string contains at least one digit character,
     *         false otherwise
     */
    private boolean isVersionString(String str) {
        if (str == null || str.isEmpty()) return false;
        for (char c : str.toCharArray()) {
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }
}