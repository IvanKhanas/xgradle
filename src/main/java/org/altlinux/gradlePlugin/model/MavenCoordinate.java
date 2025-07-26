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
package org.altlinux.gradlePlugin.model;

import java.nio.file.Path;

/**
 * Represents Maven artifact coordinates according to the Maven naming convention.
 *
 * <p>This model class contains all essential components of a Maven coordinate:
 * <ul>
 *   <li>Group identifier</li>
 *   <li>Artifact identifier</li>
 *   <li>Version</li>
 *   <li>Packaging type</li>
 *   <li>Scope</li>
 *   <li>Path to the POM file</li>
 * </ul>
 *
 * <p>Additional features:
 * <ul>
 *   <li>Validation of required fields</li>
 *   <li>BOM (Bill-of-Materials) detection</li>
 * </ul>
 */
public class MavenCoordinate {
    public String groupId;
    public String artifactId;
    public String version;
    public Path pomPath;
    public String packaging;
    public String scope;

    /**
     * Validates that the coordinate contains essential information.
     *
     * <p>Required fields:
     * <ul>
     *   <li>groupId (non-empty)</li>
     *   <li>artifactId (non-empty)</li>
     *   <li>version (non-empty)</li>
     * </ul>
     *
     * @return true if all required fields are present and non-empty,
     *         false otherwise
     */
    public boolean isValid() {
        return groupId != null && !groupId.isEmpty() &&
                artifactId != null && !artifactId.isEmpty() &&
                version != null && !version.isEmpty();
    }

    /**
     * Determines if this coordinate represents a BOM (Bill-of-Materials).
     *
     * <p>BOM artifacts are identified by their packaging type "pom".
     *
     * @return true if packaging type is "pom", false otherwise
     */
    public boolean isBom() {
        return "pom".equals(packaging);
    }
}