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
package org.altlinux.xgradle.services;

import org.altlinux.xgradle.api.PomParser;
import org.altlinux.xgradle.model.MavenCoordinate;
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

/**
 * Implementation of {@link PomParser} that parses Maven POM files
 * with caching for improved performance.
 *
 * <p>This parser provides:
 * <ul>
 *   <li>Basic POM information parsing</li>
 *   <li>Dependency management section parsing</li>
 *   <li>Dependencies parsing</li>
 *   <li>Properties parsing</li>
 *   <li>Multi-level caching for parsed results</li>
 * </ul>
 *
 * <p>Cache types:
 * <ul>
 *   <li>POM_CACHE: Main POM coordinates</li>
 *   <li>DEP_MGMT_CACHE: Dependency management entries</li>
 *   <li>DEPENDENCIES_CACHE: Direct dependencies</li>
 *   <li>PROPERTIES_CACHE: Properties section</li>
 * </ul>
 *
 * <p>Parsing handles:
 * <ul>
 *   <li>Basic project coordinates</li>
 *   <li>Inheritance from parent POM</li>
 *   <li>Default values for packaging and scope</li>
 *   <li>Namespace-agnostic element lookup</li>
 * </ul>
 *
 * @author Ivan Khanas
 */
public class DefaultPomParser implements PomParser {
    private final Map<String, MavenCoordinate> POM_CACHE = new ConcurrentHashMap<>();
    private final Map<String, ArrayList<MavenCoordinate>> DEP_MGMT_CACHE = new ConcurrentHashMap<>();
    private final Map<String, ArrayList<MavenCoordinate>> DEPENDENCIES_CACHE = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> PROPERTIES_CACHE = new ConcurrentHashMap<>();
    private final PomHierarchyLoader pomHierarchyLoader = new PomHierarchyLoader();

    /**
     * Parses the main coordinates from a POM file.
     *
     * <p>Handles:
     * <ul>
     *   <li>Direct child elements (artifactId, groupId, version)</li>
     *   <li>Parent inheritance when groupId/version are missing</li>
     *   <li>Default packaging type (jar)</li>
     *   <li>Validation of required fields</li>
     * </ul>
     *
     * @param pomPath path to the POM file
     * @param logger logger for error reporting
     *
     * @return MavenCoordinate object with parsed data, or null if parsing fails
     */
    @Override
    public MavenCoordinate parsePom(Path pomPath, Logger logger) {
        String cacheKey = pomPath.toString();
        return POM_CACHE.computeIfAbsent(cacheKey, k -> {
            List<Model> hierarchy = pomHierarchyLoader.loadHierarchy(pomPath, logger);
            if (hierarchy.isEmpty()) return null;
            return createCoordinate(hierarchy.get(hierarchy.size() - 1), pomPath);
        });
    }

    /**
     * Creates a {@link MavenCoordinate} object from a Maven {@link Model}.
     * <p>
     * Handles missing values by applying defaults or inheriting from parent POM:
     * <ul>
     *   <li>If {@code groupId} is missing, it is inherited from the parent</li>
     *   <li>If {@code version} is missing, it is inherited from the parent</li>
     *   <li>If {@code packaging} is missing, defaults to {@code jar}</li>
     * </ul>
     *
     * @param model   parsed Maven model
     * @param pomPath path to the POM file
     *
     * @return fully populated MavenCoordinate
     */
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

    /**
     * Parses properties section of a POM file.
     *
     * <p>Collects all properties declared within:
     * {@code <properties>}
     *
     * @param pomPath path to the POM file
     * @param logger logger for error reporting
     *
     * @return map of property names to values {@code @NotNull}
     */
    @Override
    public Map<String, String> parseProperties(Path pomPath, Logger logger) {
        String cacheKey = pomPath.toString();
        return PROPERTIES_CACHE.computeIfAbsent(cacheKey, k -> {
            List<Model> hierarchy = pomHierarchyLoader.loadHierarchy(pomPath, logger);
            return collectProperties(hierarchy);
        });
    }

    /**
     * Collects and merges all properties from a POM hierarchy.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Defaults to UTF-8 encodings for build/reporting if missing</li>
     *   <li>Populates project coordinates (groupId, artifactId, version, packaging)</li>
     *   <li>Merges user-defined properties from each POM {@code <properties>} section</li>
     *   <li>Child values do not overwrite already set parent values</li>
     * </ul>
     *
     * @param hierarchy list of Maven models (ordered parent -> child)
     *
     * @return merged properties map
     */
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

    /**
     * Adds a property to the map only if the value is non-null and not empty.
     *
     * @param map   target property map
     * @param key   property name
     * @param value property value (may be null or empty)
     */
    private void putIfNotEmpty(Map<String, String> map, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            map.putIfAbsent(key, value);
        }
    }

    /**
     * Parses the dependency management section of a POM file.
     *
     * <p>Only processes dependencies declared within:
     * {@code <dependencyManagement><dependencies>}
     *
     * @param pomPath path to the POM file
     * @param logger logger for error reporting
     *
     * @return list of managed dependencies {@code @NotNull}
     */
    @Override
    public ArrayList<MavenCoordinate> parseDependencyManagement(Path pomPath, Logger logger) {
        String cacheKey = pomPath.toString();
        return DEP_MGMT_CACHE.computeIfAbsent(cacheKey, k -> {
            List<Model> hierarchy = pomHierarchyLoader.loadHierarchy(pomPath, logger);
            if (hierarchy.isEmpty()) return new ArrayList<>();
            return collectDependencyManagement(hierarchy, collectProperties(hierarchy));
        });
    }

    /**
     * Collects all managed dependencies from a POM hierarchy.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Iterates through each model in the hierarchy</li>
     *   <li>Extracts dependencies from {@code <dependencyManagement>}</li>
     *   <li>Resolves property placeholders in dependency coordinates</li>
     *   <li>Uses {@code groupId:artifactId} as a key to ensure uniqueness</li>
     *   <li>Child overrides replace parent-managed entries</li>
     * </ul>
     *
     * @param hierarchy  list of Maven models (ordered parent -> child)
     * @param properties resolved property map
     *
     * @return list of managed dependencies
     */
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

    /**
     * Parses direct dependencies from a POM file.
     *
     * <p>Processes dependencies declared within:
     * {@code <dependencies>}
     *
     * @param pomPath path to the POM file
     * @param logger logger for error reporting
     *
     * @return list of direct dependencies {@code @NotNull}
     */
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

    /**
     * Extracts all direct dependencies from a POM hierarchy.
     * <p>
     * Behavior:
     * <ul>
     *   <li>Iterates through each POM model in the hierarchy</li>
     *   <li>Collects {@code <dependencies>} entries</li>
     *   <li>Resolves property placeholders</li>
     *   <li>Applies dependency management overrides (version, scope, packaging)</li>
     *   <li>Ensures uniqueness by {@code groupId:artifactId}, child dependencies override parent ones</li>
     * </ul>
     *
     * @param hierarchy  list of Maven models (ordered parent -> child)
     * @param properties resolved property map
     * @param depMgmtMap map of managed dependencies ({@code groupId:artifactId -> MavenCoordinate})
     *
     * @return list of resolved direct dependencies
     */
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

    /**
     * Applies dependency management overrides (from {@code <dependencyManagement>})
     * to the given dependency.
     * <p>
     * If a dependency in {@code <dependencies>} does not specify version/scope/packaging,
     * this method fills them in from the corresponding managed dependency.
     *
     * @param coord      dependency to adjust
     * @param depMgmtMap map of managed dependencies
     */
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

    /**
     * Resolves placeholders in the given string using provided properties.
     * <p>
     * Example:
     * <pre>
     *   version = "${project.version}"
     *   properties["project.version"] = "1.2.3"
     *   result = "1.2.3"
     * </pre>
     *
     * <p>Supports nested resolution up to 20 iterations.</p>
     *
     * @param value      raw string (may contain placeholders like ${...})
     * @param properties property map for substitution
     *
     * @return resolved string with placeholders replaced, or the original string if not resolvable
     */
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

    /**
     * Converts a Maven {@link Dependency} into a {@link MavenCoordinate}.
     * <p>
     * Applies default values:
     * <ul>
     *   <li>Scope defaults to {@code compile}</li>
     *   <li>Packaging defaults to {@code jar}</li>
     * </ul>
     *
     * @param dep Maven dependency element
     *
     * @return MavenCoordinate with basic fields populated
     */
    private MavenCoordinate convertDependency(Dependency dep) {
        MavenCoordinate coord = new MavenCoordinate();
        coord.setGroupId(dep.getGroupId());
        coord.setArtifactId(dep.getArtifactId());
        coord.setVersion(dep.getVersion());
        coord.setScope(dep.getScope() != null ? dep.getScope() : "compile");
        coord.setPackaging(dep.getType() != null ? dep.getType() : "jar");
        return coord;
    }

    /**
     * Checks if the given string is {@code null} or empty after trimming.
     *
     * @param str string to check
     * @return true if empty, false otherwise
     */
    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}