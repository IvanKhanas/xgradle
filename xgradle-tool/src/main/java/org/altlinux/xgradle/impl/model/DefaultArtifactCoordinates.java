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
package org.altlinux.xgradle.impl.model;

import org.altlinux.xgradle.interfaces.model.ArtifactCoordinates;

import java.util.Objects;

/**
 * Represents Maven artifact coordinates (groupId, artifactId, version).
 * Implements {@link ArtifactCoordinates}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class DefaultArtifactCoordinates implements ArtifactCoordinates {

    private final String groupId;
    private final String artifactId;
    private final String version;

    DefaultArtifactCoordinates(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultArtifactCoordinates that = (DefaultArtifactCoordinates) o;
        return  Objects.equals(groupId, that.groupId) &&
                Objects.equals(artifactId, that.artifactId) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }
}
