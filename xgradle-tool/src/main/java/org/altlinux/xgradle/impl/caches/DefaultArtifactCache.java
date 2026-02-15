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
package org.altlinux.xgradle.impl.caches;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.caches.ArtifactCache;
import org.altlinux.xgradle.interfaces.model.ArtifactCoordinates;
import org.altlinux.xgradle.interfaces.model.ArtifactData;

/**
 * Default implementation of ArtifactCache.
 * Implements {@link ArtifactCache}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultArtifactCache implements ArtifactCache {
    private final Cache<ArtifactCoordinates, ArtifactData> artifactCache =
            CacheBuilder.newBuilder().build();

    @Override
    public boolean contains(ArtifactCoordinates coordinates) {
        return artifactCache.getIfPresent(coordinates) != null;
    }

    @Override
    public boolean add(ArtifactData artifactData) {
        return artifactCache
                .asMap()
                .putIfAbsent(artifactData.getCoordinates(), artifactData) == null;
    }

    @Override
    public ArtifactData get(ArtifactCoordinates coordinates) {
        return artifactCache.getIfPresent(coordinates);
    }

    @Override
    public int size() {
        long sz = artifactCache.size();
        return sz > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sz;
    }
}
