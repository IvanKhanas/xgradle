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
 * Interface for global cache tracking processed artifacts to prevent duplicates.
 * Uses artifact coordinates as unique key to ensure each artifact is processed only once.
 *
 * @author Ivan Khanas
 */
public interface ArtifactCache {

    /**
     * Checks if artifact with given coordinates is already cached.
     *
     * @param coordinates artifact coordinates to check
     * @return true if artifact is already cached, false otherwise
     */
    boolean contains(ArtifactCoordinates coordinates);

    /**
     * Adds artifact data to cache if not already present.
     *
     * @param artifactData artifact data to cache
     * @return true if artifact was added, false if it was already present
     */
    boolean add(ArtifactData artifactData);

    /**
     * Retrieves artifact data by coordinates.
     *
     * @param coordinates artifact coordinates
     * @return artifact data or null if not found
     */
    ArtifactData get(ArtifactCoordinates coordinates);

    /**
     * Gets the number of cached artifacts.
     *
     * @return number of cached artifacts
     */
    int size();
}