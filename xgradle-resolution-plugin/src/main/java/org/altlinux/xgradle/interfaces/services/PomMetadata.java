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
package org.altlinux.xgradle.interfaces.services;

import java.util.List;

/**
 * Immutable POM metadata used by SBOM generation.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public final class PomMetadata {

    private static final PomMetadata EMPTY = new PomMetadata(null, null, List.of());

    private final String projectUrl;
    private final String scmUrl;
    private final List<PomMetadataLicense> licenses;

    public PomMetadata(
            String projectUrl,
            String scmUrl,
            List<PomMetadataLicense> licenses
    ) {
        this.projectUrl = projectUrl;
        this.scmUrl = scmUrl;
        this.licenses = licenses == null ? List.of() : List.copyOf(licenses);
    }

    public static PomMetadata empty() {
        return EMPTY;
    }

    public String getProjectUrl() {
        return projectUrl;
    }

    public String getScmUrl() {
        return scmUrl;
    }

    public List<PomMetadataLicense> getLicenses() {
        return licenses;
    }
}
