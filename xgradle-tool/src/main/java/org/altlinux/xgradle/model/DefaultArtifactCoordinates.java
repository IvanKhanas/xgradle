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

import org.altlinux.xgradle.api.model.ArtifactCoordinates;

import java.util.Objects;

/**
 * Represents Maven artifact coordinates (groupId, artifactId, version).
 * Used to uniquely identify artifacts and prevent duplicates.
 *
 * @author Ivan Khanas
 */
public class DefaultArtifactCoordinates implements ArtifactCoordinates {
    private final String groupId;
    private final String artifactId;
    private final String version;

    /**
     * Constructs a new DefaultArtifactCoordinates with the specified groupId, artifactId, and version.
     *
     * @param groupId the groupId of the artifact
     * @param artifactId the artifactId of the artifact
     * @param version the version of the artifact
     */
    public DefaultArtifactCoordinates(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    /**
     * Gets the groupId of the artifact.
     *
     * @return the groupId
     */
    @Override
    public String getGroupId() {
        return groupId;
    }

    /**
     * Gets the artifactId of the artifact.
     *
     * @return the artifactId
     */
    @Override
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Gets the version of the artifact.
     *
     * @return the version
     */
    @Override
    public String getVersion() {
        return version;
    }

    /**
     * Compares this artifact coordinates to another object for equality.
     * Two artifact coordinates are equal if they have the same groupId, artifactId, and version.
     *
     * @param o the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultArtifactCoordinates that = (DefaultArtifactCoordinates) o;
        return  Objects.equals(groupId, that.groupId) &&
                Objects.equals(artifactId, that.artifactId) &&
                Objects.equals(version, that.version);
    }

    /**
     * Returns a hash code value for the artifact coordinates.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }

    /**
     * Returns a string representation of the artifact coordinates in the format "groupId:artifactId:version".
     *
     * @return string representation of the coordinates
     */
    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }
}
