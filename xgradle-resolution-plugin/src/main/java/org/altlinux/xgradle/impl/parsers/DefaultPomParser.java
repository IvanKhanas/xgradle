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
package org.altlinux.xgradle.impl.parsers;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.altlinux.xgradle.interfaces.caches.PomDataCache;
import org.altlinux.xgradle.interfaces.maven.PomHierarchyLoader;
import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.model.PomCoordinateFactory;
import org.apache.maven.model.Model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates Maven POM parsing by delegating responsibilities to specialised parser components.
 * Implements {@link PomParser}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultPomParser implements PomParser {

    private final PomDataCache cache;
    private final PomHierarchyLoader hierarchyLoader;

    private final PomPropertiesCollector propertiesCollector;
    private final PomCoordinateFactory coordinateFactory;
    private final PomDependencyManagementParser dependencyManagementParser;
    private final PomDependenciesParser dependenciesParser;

    @Inject
    DefaultPomParser(
            PomDataCache cache,
            PomHierarchyLoader hierarchyLoader
    ) {
        this.cache = cache;
        this.hierarchyLoader = hierarchyLoader;

        this.propertiesCollector = new PomPropertiesCollector();
        this.coordinateFactory = new PomCoordinateFactory();
        this.dependencyManagementParser =
                new PomDependencyManagementParser();
        this.dependenciesParser = new PomDependenciesParser();
    }

    @Override
    public MavenCoordinate parsePom(Path pomPath) {
        String cacheKey = pomPath.toString();

        MavenCoordinate cached = cache.getPom(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<Model> hierarchy = hierarchyLoader.loadHierarchy(pomPath);
        if (hierarchy == null || hierarchy.isEmpty()) {
            return null;
        }

        MavenCoordinate coordinate =
                coordinateFactory.create(
                        hierarchy.get(hierarchy.size() - 1),
                        pomPath
                );

        if (coordinate != null) {
            cache.putPom(cacheKey, coordinate);
        }
        return coordinate;
    }

    @Override
    public Map<String, String> parseProperties(Path pomPath) {
        String cacheKey = pomPath.toString();

        Map<String, String> cached = cache.getProperties(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<Model> hierarchy = hierarchyLoader.loadHierarchy(pomPath);
        Map<String, String> properties =
                Map.copyOf(
                        propertiesCollector.collect(hierarchy)
                );

        cache.putProperties(cacheKey, properties);
        return properties;
    }

    @Override
    public ArrayList<MavenCoordinate> parseDependencyManagement(Path pomPath) {
        String cacheKey = pomPath.toString();

        ImmutableList<MavenCoordinate> cached =
                cache.getDependencyManagement(cacheKey);
        if (cached != null) {
            return new ArrayList<>(cached);
        }

        List<Model> hierarchy = hierarchyLoader.loadHierarchy(pomPath);
        Map<String, String> properties =
                propertiesCollector.collect(hierarchy);

        Map<String, MavenCoordinate> managed =
                dependencyManagementParser.parse(
                        hierarchy,
                        properties,
                        propertiesCollector
                );

        ImmutableList<MavenCoordinate> result =
                ImmutableList.copyOf(managed.values());

        cache.putDependencyManagement(cacheKey, result);
        return new ArrayList<>(result);
    }

    @Override
    public ArrayList<MavenCoordinate> parseDependencies(Path pomPath) {
        String cacheKey = pomPath.toString();

        ImmutableList<MavenCoordinate> cached =
                cache.getDependencies(cacheKey);
        if (cached != null) {
            return new ArrayList<>(cached);
        }

        List<Model> hierarchy = hierarchyLoader.loadHierarchy(pomPath);
        Map<String, String> properties =
                propertiesCollector.collect(hierarchy);

        Map<String, MavenCoordinate> managed =
                dependencyManagementParser.parse(
                        hierarchy,
                        properties,
                        propertiesCollector
                );

        Map<String, MavenCoordinate> resolved =
                dependenciesParser.parse(
                        hierarchy,
                        properties,
                        managed,
                        propertiesCollector
                );

        ArrayList<MavenCoordinate> result =
                new ArrayList<>(resolved.values());

        cache.putDependencies(cacheKey, ImmutableList.copyOf(result));
        return result;
    }
}
