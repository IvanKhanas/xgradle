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
package org.altlinux.gradlePlugin.services;

import org.altlinux.gradlePlugin.api.PomParser;
import org.altlinux.gradlePlugin.model.MavenCoordinate;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.gradle.api.logging.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultPomParser implements PomParser {
    private final Map<String, MavenCoordinate> POM_CACHE = new ConcurrentHashMap<>();
    private final Map<String, ArrayList<MavenCoordinate>> DEP_MGMT_CACHE = new ConcurrentHashMap<>();
    private final Map<String, ArrayList<MavenCoordinate>> DEPENDENCIES_CACHE = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> PROPERTIES_CACHE = new ConcurrentHashMap<>();
    private final PomHierarchyLoader pomHierarchyLoader = new PomHierarchyLoader();

    @Override
    public MavenCoordinate parsePom(Path pomPath, Logger logger) {
        String cacheKey = pomPath.toString();
        return POM_CACHE.computeIfAbsent(cacheKey, k -> {
            List<Model> hierarchy = pomHierarchyLoader.loadHierarchy(pomPath, logger);
            if (hierarchy.isEmpty()) return null;
            return createCoordinate(hierarchy.get(hierarchy.size() - 1), pomPath);
        });
    }

    private MavenCoordinate createCoordinate(Model model, Path pomPath) {
        MavenCoordinate coord = new MavenCoordinate();
        coord.setPomPath(pomPath);
        coord.setArtifactId(model.getArtifactId());
        coord.setVersion(model.getVersion());
        coord.setGroupId(model.getGroupId());
        coord.setPackaging(model.getPackaging());

        if (isEmpty(coord.getGroupId()) && model.getParent() != null) {
            coord.setGroupId(model.getParent().getGroupId());
        }
        if (isEmpty(coord.getVersion()) && model.getParent() != null) {
            coord.setVersion(model.getParent().getVersion());
        }
        if (isEmpty(coord.getPackaging())) {
            coord.setPackaging("jar");
        }
        return coord;
    }

    @Override
    public Map<String, String> parseProperties(Path pomPath, Logger logger) {
        String cacheKey = pomPath.toString();
        return PROPERTIES_CACHE.computeIfAbsent(cacheKey, k -> {
            List<Model> hierarchy = pomHierarchyLoader.loadHierarchy(pomPath, logger);
            return collectProperties(hierarchy);
        });
    }

    private Map<String, String> collectProperties(List<Model> hierarchy) {
        Map<String, String> props = new HashMap<>();
        props.putIfAbsent("project.build.sourceEncoding", "UTF-8");
        props.putIfAbsent("project.reporting.outputEncoding", "UTF-8");

        for (Model model : hierarchy) {
            putIfNotEmpty(props, "project.groupId", model.getGroupId());
            putIfNotEmpty(props, "groupId", model.getGroupId());
            putIfNotEmpty(props, "project.artifactId", model.getArtifactId());
            putIfNotEmpty(props, "artifactId", model.getArtifactId());
            putIfNotEmpty(props, "project.version", model.getVersion());
            putIfNotEmpty(props, "version", model.getVersion());
            putIfNotEmpty(props, "project.packaging", model.getPackaging());
            putIfNotEmpty(props, "packaging", model.getPackaging());

            if (model.getProperties() != null) {
                model.getProperties().forEach((k, v) ->
                        props.put(k.toString(), v.toString()));
            }
        }
        return props;
    }

    private void putIfNotEmpty(Map<String, String> map, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            map.putIfAbsent(key, value);
        }
    }

    @Override
    public ArrayList<MavenCoordinate> parseDependencyManagement(Path pomPath, Logger logger) {
        String cacheKey = pomPath.toString();
        return DEP_MGMT_CACHE.computeIfAbsent(cacheKey, k -> {
            List<Model> hierarchy = pomHierarchyLoader.loadHierarchy(pomPath, logger);
            if (hierarchy.isEmpty()) return new ArrayList<>();
            return collectDependencyManagement(hierarchy, collectProperties(hierarchy));
        });
    }

    private ArrayList<MavenCoordinate> collectDependencyManagement(
            List<Model> hierarchy, Map<String, String> properties
    ) {
        Map<String, MavenCoordinate> depMgmtMap = new LinkedHashMap<>();
        for (Model model : hierarchy) {
            if (model.getDependencyManagement() == null) continue;
            for (Dependency dep : model.getDependencyManagement().getDependencies()) {
                MavenCoordinate coord = convertDependency(dep);
                resolveProperties(coord, properties);
                if (coord.isValid()) {
                    depMgmtMap.put(coord.getGroupId() + ":" + coord.getArtifactId(), coord);
                }
            }
        }
        return new ArrayList<>(depMgmtMap.values());
    }

    @Override
    public ArrayList<MavenCoordinate> parseDependencies(Path pomPath, Logger logger) {
        String cacheKey = pomPath.toString();
        return DEPENDENCIES_CACHE.computeIfAbsent(cacheKey, k -> {
            List<Model> hierarchy = pomHierarchyLoader.loadHierarchy(pomPath, logger);
            if (hierarchy.isEmpty()) return new ArrayList<>();

            Map<String, String> properties = collectProperties(hierarchy);
            Map<String, MavenCoordinate> depMgmtMap = new HashMap<>();
            for (MavenCoordinate coord : collectDependencyManagement(hierarchy, properties)) {
                if (coord.getGroupId() != null && coord.getArtifactId() != null) {
                    depMgmtMap.put(coord.getGroupId() + ":" + coord.getArtifactId(), coord);
                }
            }
            return extractAllDependencies(hierarchy, properties, depMgmtMap);
        });
    }

    private ArrayList<MavenCoordinate> extractAllDependencies(
            List<Model> hierarchy,
            Map<String, String> properties,
            Map<String, MavenCoordinate> depMgmtMap
    ) {
        Map<String, MavenCoordinate> allDeps = new LinkedHashMap<>();
        for (Model model : hierarchy) {
            for (Dependency dep : model.getDependencies()) {
                MavenCoordinate coord = convertDependency(dep);
                resolveProperties(coord, properties);
                if (coord.getGroupId() != null && coord.getArtifactId() != null) {
                    allDeps.put(coord.getGroupId() + ":" + coord.getArtifactId(), coord);
                }
            }
        }

        ArrayList<MavenCoordinate> result = new ArrayList<>();
        for (MavenCoordinate coord : allDeps.values()) {
            applyDependencyManagement(coord, depMgmtMap);
            if (coord.isValid()) result.add(coord);
        }
        return result;
    }

    private void applyDependencyManagement(
            MavenCoordinate coord, Map<String, MavenCoordinate> depMgmtMap
    ) {
        if (coord.getGroupId() == null || coord.getArtifactId() == null) return;

        String key = coord.getGroupId() + ":" + coord.getArtifactId();
        MavenCoordinate managed = depMgmtMap.get(key);
        if (managed == null) return;

        if (isEmpty(coord.getVersion())) coord.setVersion(managed.getVersion());
        if (isEmpty(coord.getScope())) coord.setScope(managed.getScope());
        if (isEmpty(coord.getPackaging())) coord.setPackaging(managed.getPackaging());
    }

    private void resolveProperties(MavenCoordinate coord, Map<String, String> properties) {
        coord.setGroupId(resolveProperty(coord.getGroupId(), properties));
        coord.setArtifactId(resolveProperty(coord.getArtifactId(), properties));
        coord.setVersion(resolveProperty(coord.getVersion(), properties));
        coord.setPackaging(resolveProperty(coord.getPackaging(), properties));
        coord.setScope(resolveProperty(coord.getScope(), properties));
    }

    private String resolveProperty(String value, Map<String, String> properties) {
        if (value == null) return null;

        String current = value;
        for (int i = 0; i < 20; i++) {
            StringBuilder builder = new StringBuilder();
            int startIndex = 0;
            boolean changed = false;

            while (startIndex < current.length()) {
                int beginIndex = current.indexOf("${", startIndex);
                if (beginIndex == -1) {
                    builder.append(current.substring(startIndex));
                    break;
                }

                int endIndex = current.indexOf('}', beginIndex + 2);
                if (endIndex == -1) {
                    builder.append(current.substring(startIndex));
                    break;
                }

                builder.append(current, startIndex, beginIndex);
                String key = current.substring(beginIndex + 2, endIndex);
                String replacement = properties.get(key);

                if (replacement != null) {
                    builder.append(replacement);
                    changed = true;
                } else {
                    builder.append("${").append(key).append("}");
                }
                startIndex = endIndex + 1;
            }

            if (!changed) break;
            current = builder.toString();
        }
        return current;
    }

    private MavenCoordinate convertDependency(Dependency dep) {
        MavenCoordinate coord = new MavenCoordinate();
        coord.setGroupId(dep.getGroupId());
        coord.setArtifactId(dep.getArtifactId());
        coord.setVersion(dep.getVersion());
        coord.setScope(dep.getScope() != null ? dep.getScope() : "compile");
        coord.setPackaging(dep.getType() != null ? dep.getType() : "jar");
        return coord;
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}