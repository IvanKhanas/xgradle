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
import com.google.common.collect.ImmutableList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.caches.PomDataCache;
import org.altlinux.xgradle.impl.model.MavenCoordinate;

import org.gradle.api.logging.Logger;

import java.util.Map;
import java.util.concurrent.TimeUnit;
/**
 * Cache for POM Data.
 * Implements {@link PomDataCache}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

@Singleton
public final class DefaultPomDataCache implements PomDataCache {

    private final Cache<String, MavenCoordinate> pomCache;
    private final Cache<String, ImmutableList<MavenCoordinate>> depMgmtCache;
    private final Cache<String, ImmutableList<MavenCoordinate>> dependenciesCache;
    private final Cache<String, Map<String, String>> propertiesCache;

    @Inject
    public DefaultPomDataCache() {
        this.pomCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .recordStats()
                .build();

        this.depMgmtCache = CacheBuilder.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build();

        this.dependenciesCache = CacheBuilder.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build();

        this.propertiesCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Override
    public MavenCoordinate getPom(String key) {
        return pomCache.getIfPresent(key);
    }

    @Override
    public void putPom(String key, MavenCoordinate coordinate) {
        if (coordinate != null) {
            pomCache.put(key, coordinate);
        }
    }

    @Override
    public void invalidatePom(String key) {
        pomCache.invalidate(key);
    }

    @Override
    public ImmutableList<MavenCoordinate> getDependencyManagement(String key) {
        return depMgmtCache.getIfPresent(key);
    }

    @Override
    public void putDependencyManagement(String key, ImmutableList<MavenCoordinate> dependencies) {
        if (dependencies != null && !dependencies.isEmpty()) {
            depMgmtCache.put(key, dependencies);
        }
    }

    @Override
    public ImmutableList<MavenCoordinate> getDependencies(String key) {
        return dependenciesCache.getIfPresent(key);
    }

    @Override
    public void putDependencies(String key, ImmutableList<MavenCoordinate> dependencies) {
        if (dependencies != null && !dependencies.isEmpty()) {
            dependenciesCache.put(key, dependencies);
        }
    }

    @Override
    public Map<String, String> getProperties(String key) {
        return propertiesCache.getIfPresent(key);
    }

    @Override
    public void putProperties(String key, Map<String, String> properties) {
        if (properties != null && !properties.isEmpty()) {
            propertiesCache.put(key, properties);
        }
    }

    @Override
    public void logStats(Logger logger) {
        logger.debug("POM Cache Stats: {}", pomCache.stats());
        logger.debug("DependencyManagement Cache Stats: {}", depMgmtCache.stats());
        logger.debug("Dependencies Cache Stats: {}", dependenciesCache.stats());
        logger.debug("Properties Cache Stats: {}", propertiesCache.stats());
    }
}
