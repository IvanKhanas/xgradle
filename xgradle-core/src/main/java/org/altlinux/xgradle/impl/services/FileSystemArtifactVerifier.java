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

import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.services.ArtifactVerifier;
import org.altlinux.xgradle.impl.enums.MavenPackaging;
import org.altlinux.xgradle.impl.extensions.SystemDepsExtension;
import org.altlinux.xgradle.impl.model.MavenCoordinate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Implementation of {@link ArtifactVerifier} that checks for the physical presence of artifact files in the local filesystem.
 * Implements {@link ArtifactVerifier}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
class FileSystemArtifactVerifier implements ArtifactVerifier {

    @Override
    public boolean verifyArtifactExists(MavenCoordinate coord) {
        if (coord == null || !coord.isValid()) {
            return false;
        }

        if (MavenPackaging.POM.getPackaging().equals(coord.getPackaging())) {
            return true;
        }

        Path basePath = Paths.get(SystemDepsExtension.getJarsPath());
        String basePattern = coord.getArtifactId();
        String versionedPattern = coord.getArtifactId() + "-" + coord.getVersion();

        return checkArtifactExists(basePath, basePattern + ".jar") ||
                checkArtifactExists(basePath, versionedPattern + ".jar") ||
                checkRecursively(basePath, basePattern, coord.getVersion());
    }

    private boolean checkArtifactExists(Path baseDir, String fileName) {
        Path filePath = baseDir.resolve(fileName);
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }

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
