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

import com.google.inject.Singleton;

import org.altlinux.xgradle.api.model.ArtifactCache;
import org.altlinux.xgradle.api.model.ArtifactCoordinates;
import org.altlinux.xgradle.api.model.ArtifactData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of ArtifactCache.
 * Global cache for tracking processed artifacts to prevent duplicates.
 *
 * @author Ivan Khanas
 */
@Singleton
public class DefaultArtifactCache implements ArtifactCache {
    private final Map<ArtifactCoordinates, ArtifactData> artifactCache = new ConcurrentHashMap<>();

    /**
     * Checks if the cache contains an artifact with the given coordinates.
     *
     * @param coordinates artifact coordinates to check
     * @return true if the cache contains the artifact, false otherwise
     */
    @Override
    public boolean contains(ArtifactCoordinates coordinates) {
        return artifactCache.containsKey(coordinates);
    }

    /**
     * Adds an artifact to the cache if it is not already present.
     *
     * @param artifactData artifact data to add
     * @return true if the artifact was added, false if it was already present
     */
    @Override
    public boolean add(ArtifactData artifactData) {
        return artifactCache.putIfAbsent(artifactData.getCoordinates(), artifactData) == null;
    }

    /**
     * Retrieves the artifact data for the given coordinates.
     *
     * @param coordinates artifact coordinates
     * @return the artifact data, or null if not found
     */
    @Override
    public ArtifactData get(ArtifactCoordinates coordinates) {
        return artifactCache.get(coordinates);
    }

    /**
     * Returns the number of artifacts in the cache.
     *
     * @return the number of cached artifacts
     */
    @Override
    public int size() {
        return artifactCache.size();
    }
}