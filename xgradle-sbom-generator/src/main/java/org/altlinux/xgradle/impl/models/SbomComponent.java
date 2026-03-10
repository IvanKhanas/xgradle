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
package org.altlinux.xgradle.impl.models;

import org.altlinux.xgradle.impl.enums.SbomComponentKind;

import java.util.List;

/**
 * Represents a normalized SBOM component entry.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public final class SbomComponent {

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String fileName;
    private final String projectUrl;
    private final String scmUrl;
    private final SbomComponentKind componentKind;
    private final List<SbomLicense> licenses;

    private SbomComponent(
            String groupId,
            String artifactId,
            String version,
            String fileName,
            String projectUrl,
            String scmUrl,
            SbomComponentKind componentKind,
            List<SbomLicense> licenses
    ) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.fileName = fileName;
        this.projectUrl = projectUrl;
        this.scmUrl = scmUrl;
        this.componentKind = componentKind != null ? componentKind : SbomComponentKind.LIBRARY;
        this.licenses = licenses == null ? List.of() : List.copyOf(licenses);
    }

    public static SbomComponent maven(String groupId, String artifactId, String version) {
        return new SbomComponent(
                groupId,
                artifactId,
                version,
                null,
                null,
                null,
                SbomComponentKind.LIBRARY,
                List.of()
        );
    }

    public static SbomComponent maven(
            String groupId,
            String artifactId,
            String version,
            String projectUrl,
            String scmUrl,
            List<SbomLicense> licenses
    ) {
        return new SbomComponent(
                groupId,
                artifactId,
                version,
                null,
                projectUrl,
                scmUrl,
                SbomComponentKind.LIBRARY,
                licenses
        );
    }

    public static SbomComponent mavenPlugin(String groupId, String artifactId, String version) {
        return new SbomComponent(
                groupId,
                artifactId,
                version,
                null,
                null,
                null,
                SbomComponentKind.GRADLE_PLUGIN,
                List.of()
        );
    }

    public static SbomComponent mavenPlugin(
            String groupId,
            String artifactId,
            String version,
            String projectUrl,
            String scmUrl,
            List<SbomLicense> licenses
    ) {
        return new SbomComponent(
                groupId,
                artifactId,
                version,
                null,
                projectUrl,
                scmUrl,
                SbomComponentKind.GRADLE_PLUGIN,
                licenses
        );
    }

    public static SbomComponent file(String fileName) {
        return new SbomComponent(
                null,
                null,
                null,
                fileName,
                null,
                null,
                SbomComponentKind.FILE,
                List.of()
        );
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getFileName() {
        return fileName;
    }

    public String getProjectUrl() {
        return projectUrl;
    }

    public String getScmUrl() {
        return scmUrl;
    }

    public SbomComponentKind getComponentKind() {
        return componentKind;
    }

    public List<SbomLicense> getLicenses() {
        return licenses;
    }

    public String displayName() {
        if (groupId != null && artifactId != null) {
            return groupId + ":" + artifactId;
        }
        return fileName != null ? fileName : "unknown";
    }

    public String uniqueKey() {
        if (groupId != null && artifactId != null) {
            return groupId + ":" + artifactId + ":" + (version != null ? version : "");
        }
        return "file:" + (fileName != null ? fileName : "unknown");
    }
}
