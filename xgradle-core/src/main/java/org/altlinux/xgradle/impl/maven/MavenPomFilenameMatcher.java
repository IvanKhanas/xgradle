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
package org.altlinux.xgradle.impl.maven;

import com.google.inject.Singleton;
import org.altlinux.xgradle.interfaces.maven.PomFilenameMatcher;
import org.gradle.util.internal.VersionNumber;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Helper for matching POM filenames to Maven artifacts and versions.
 * Implements {@link PomFilenameMatcher}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class MavenPomFilenameMatcher implements PomFilenameMatcher {

    @Override
    public boolean matches(Path path, String variant, String artifactId) {
        String fileName = path.getFileName().toString();
        if (!fileName.endsWith(".pom")) {
            return false;
        }

        String baseName = fileName.substring(0, fileName.length() - 4);

        if (baseName.equals(variant)) {
            return true;
        }

        if (baseName.startsWith(variant + "-")) {
            String suffix = baseName.substring(variant.length() + 1);
            return isVersionString(suffix) || isVersionedArtifact(suffix, artifactId);
        }

        return false;
    }

    @Override
    public Set<String> generateNameVariants(String groupId, String artifactId) {
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

    private boolean isVersionedArtifact(String suffix, String artifactId) {
        if (!suffix.endsWith(artifactId)) {
            return false;
        }

        if (suffix.length() <= artifactId.length() + 1) {
            return false;
        }

        String versionPart = suffix.substring(
                0,
                suffix.length() - artifactId.length() - 1
        );

        return isVersionString(versionPart);
    }

    private boolean isVersionString(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        if (!Character.isDigit(str.charAt(0))) {
            return false;
        }

        try {
            VersionNumber version = VersionNumber.parse(str);
            return !VersionNumber.UNKNOWN.equals(version);
        } catch (Exception e) {
            return false;
        }
    }
}
