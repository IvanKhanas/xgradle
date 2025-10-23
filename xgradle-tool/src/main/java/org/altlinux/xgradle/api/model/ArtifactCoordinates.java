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
package org.altlinux.xgradle.api.model;

/**
 * Interface representing Maven artifact coordinates (groupId, artifactId, version).
 * Used to uniquely identify artifacts and prevent duplicates.
 *
 * @author Ivan Khanas
 */
public interface ArtifactCoordinates {

    /**
     * Gets the groupId of the artifact.
     *
     * @return the groupId
     */
    String getGroupId();

    /**
     * Gets the artifactId of the artifact.
     *
     * @return the artifactId
     */
    String getArtifactId();

    /**
     * Gets the version of the artifact.
     *
     * @return the version
     */
    String getVersion();

    /**
     * Returns string representation of coordinates in groupId:artifactId:version format.
     *
     * @return string representation of coordinates
     */
    String toString();
}