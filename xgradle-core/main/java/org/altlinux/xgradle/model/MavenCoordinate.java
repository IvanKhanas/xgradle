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
package org.altlinux.xgradle.model;

import java.nio.file.Path;
import java.util.Objects;

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
 *
 * @author Ivan Khanas
 */
public class MavenCoordinate {

    private String groupId;
    private String artifactId;
    private String version;
    private Path pomPath;
    private String packaging;
    private String scope;
    private boolean testContext;

    /**
     * Default no-argument constructor.
     *
     * <p>Creates an empty MavenCoordinate instance.
     * All fields must be set explicitly using setters before
     * the coordinate is considered valid.</p>
     */
    public MavenCoordinate () {}

    /**
     * Copy constructor.
     *
     * <p>Creates a new {@code MavenCoordinate} instance by copying
     * all fields from another instance.</p>
     *
     * @param other the MavenCoordinate to copy
     */
    public MavenCoordinate(MavenCoordinate other) {
        this.groupId = other.groupId;
        this.artifactId = other.artifactId;
        this.version = other.version;
        this.scope = other.scope;
        this.packaging = other.packaging;
        this.pomPath = other.pomPath;
        this.testContext = other.testContext;
    }

    /**
     * Compares this MavenCoordinate to another object for equality.
     *
     * <p>Two coordinates are considered equal if they share the same
     * {@code groupId} and {@code artifactId}, regardless of version,
     * packaging, or scope.</p>
     *
     * @param o the object to compare with
     * @return {@code true} if the objects represent the same artifact identity,
     *         {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MavenCoordinate that = (MavenCoordinate) o;
        return Objects.equals(groupId, that.groupId) &&
                Objects.equals(artifactId, that.artifactId);
    }

    /**
     * Computes the hash code for this MavenCoordinate.
     *
     * <p>The hash code is derived from {@code groupId} and {@code artifactId}
     * to remain consistent with {@link #equals(Object)}.</p>
     *
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId);
    }

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

    /**
     * Returns the group identifier of the artifact.
     *
     * @return group identifier string
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Sets the group identifier of the artifact.
     *
     * @param groupId new group identifier
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * Returns the scope of the artifact.
     *
     * @return scope string
     */
    public String getScope() {
        return scope;
    }

    /**
     * Sets the scope of the artifact.
     *
     * @param scope new scope value
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * Returns the packaging type of the artifact.
     *
     * @return packaging type string
     */
    public String getPackaging() {
        return packaging;
    }

    /**
     * Sets the packaging type of the artifact.
     *
     * @param packaging new packaging type
     */
    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    /**
     * Returns the path to the POM file.
     *
     * @return path to POM file
     */
    public Path getPomPath() {
        return pomPath;
    }

    /**
     * Sets the path to the POM file.
     *
     * @param pomPath new POM file path
     */
    public void setPomPath(Path pomPath) {
        this.pomPath = pomPath;
    }

    /**
     * Returns the version of the artifact.
     *
     * @return version string
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the version of the artifact.
     *
     * @param version new version value
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns the artifact identifier.
     *
     * @return artifact ID string
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Sets the artifact identifier.
     *
     * @param artifactId new artifact ID
     */
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * Indicates whether this artifact is explicitly marked as a test-context dependency.
     *
     * <p>Test-context artifacts are typically resolved only for testing and
     * are not included in production configurations.</p>
     *
     * @return {@code true} if this artifact is in the test context, {@code false} otherwise
     */
    public boolean isTestContext() {
        return testContext;
    }

    /**
     * Marks this artifact as belonging to the test context.
     *
     * @param testContext {@code true} if the artifact is test-related, {@code false} otherwise
     */
    public void setTestContext(boolean testContext) {
        this.testContext = testContext;
    }
}