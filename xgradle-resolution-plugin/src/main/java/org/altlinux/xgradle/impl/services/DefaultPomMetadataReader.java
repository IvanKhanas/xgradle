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
package org.altlinux.xgradle.impl.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.maven.PomHierarchyLoader;
import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.altlinux.xgradle.interfaces.services.PomMetadata;
import org.altlinux.xgradle.interfaces.services.PomMetadataLicense;
import org.altlinux.xgradle.interfaces.services.PomMetadataReader;

import org.apache.maven.model.Model;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Reads metadata required for SBOM enrichment from Maven POM hierarchy.
 * Implements {@link PomMetadataReader}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultPomMetadataReader implements PomMetadataReader {

    private final PomHierarchyLoader hierarchyLoader;
    private final PomParser pomParser;

    @Inject
    DefaultPomMetadataReader(
            PomHierarchyLoader hierarchyLoader,
            PomParser pomParser
    ) {
        this.hierarchyLoader = hierarchyLoader;
        this.pomParser = pomParser;
    }

    @Override
    public PomMetadata read(Path pomPath) {
        if (pomPath == null) {
            return PomMetadata.empty();
        }

        List<Model> hierarchy = hierarchyLoader.loadHierarchy(pomPath);
        if (hierarchy == null || hierarchy.isEmpty()) {
            return PomMetadata.empty();
        }

        Map<String, String> properties = buildResolutionProperties(pomPath, hierarchy);

        return new PomMetadata(
                resolveProjectUrl(hierarchy, properties),
                resolveScmUrl(hierarchy, properties),
                resolveLicenses(hierarchy, properties)
        );
    }

    private Map<String, String> buildResolutionProperties(
            Path pomPath,
            List<Model> hierarchy
    ) {
        Map<String, String> properties = new LinkedHashMap<>(pomParser.parseProperties(pomPath));

        String groupId = findFirstFromChild(hierarchy, Model::getGroupId);
        String artifactId = findFirstFromChild(hierarchy, Model::getArtifactId);
        String version = findFirstFromChild(hierarchy, Model::getVersion);
        String packaging = findFirstFromChild(hierarchy, Model::getPackaging);
        String scmTag = findFirstScmTagFromChild(hierarchy);

        putIfNotEmpty(properties, "project.groupId", groupId);
        putIfNotEmpty(properties, "groupId", groupId);
        putIfNotEmpty(properties, "project.artifactId", artifactId);
        putIfNotEmpty(properties, "artifactId", artifactId);
        putIfNotEmpty(properties, "project.version", version);
        putIfNotEmpty(properties, "version", version);
        putIfNotEmpty(properties, "project.packaging", packaging);
        putIfNotEmpty(properties, "packaging", packaging);
        putIfNotEmpty(properties, "project.scm.tag", scmTag);
        putIfNotEmpty(properties, "scm.tag", scmTag);

        return properties;
    }

    private String resolveProjectUrl(
            List<Model> hierarchy,
            Map<String, String> properties
    ) {
        return normalizeResolved(findFirstFromChild(hierarchy, Model::getUrl), properties);
    }

    private String resolveScmUrl(
            List<Model> hierarchy,
            Map<String, String> properties
    ) {
        return IntStream.iterate(hierarchy.size() - 1, index -> index >= 0, index -> index - 1)
                .mapToObj(hierarchy::get)
                .filter(model -> model != null && model.getScm() != null)
                .map(model -> normalizeResolved(
                        firstNonEmpty(
                                model.getScm().getUrl(),
                                model.getScm().getConnection(),
                                model.getScm().getDeveloperConnection()
                        ),
                        properties
                ))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private List<PomMetadataLicense> resolveLicenses(
            List<Model> hierarchy,
            Map<String, String> properties
    ) {
        return IntStream.iterate(hierarchy.size() - 1, index -> index >= 0, index -> index - 1)
                .mapToObj(hierarchy::get)
                .filter(model -> model != null && model.getLicenses() != null && !model.getLicenses().isEmpty())
                .map(model -> model.getLicenses().stream()
                        .filter(license -> license != null)
                        .map(license -> new PomMetadataLicense(
                                normalizeResolved(license.getName(), properties),
                                normalizeResolved(license.getUrl(), properties)
                        ))
                        .filter(pomLicense -> pomLicense.getName() != null || pomLicense.getUrl() != null)
                        .collect(Collectors.toList()))
                .filter(resolved -> !resolved.isEmpty())
                .findFirst()
                .map(List::copyOf)
                .orElse(List.of());
    }

    private String findFirstFromChild(
            List<Model> hierarchy,
            Function<Model, String> extractor
    ) {
        return IntStream.iterate(hierarchy.size() - 1, index -> index >= 0, index -> index - 1)
                .mapToObj(hierarchy::get)
                .filter(model -> model != null)
                .map(extractor)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String findFirstScmTagFromChild(List<Model> hierarchy) {
        return IntStream.iterate(hierarchy.size() - 1, index -> index >= 0, index -> index - 1)
                .mapToObj(hierarchy::get)
                .filter(model -> model != null && model.getScm() != null)
                .map(model -> model.getScm().getTag())
                .filter(tag -> tag != null && !tag.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String normalizeResolved(
            String value,
            Map<String, String> properties
    ) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        return normalize(resolvePlaceholders(normalized, properties));
    }

    private String resolvePlaceholders(
            String value,
            Map<String, String> properties
    ) {
        if (value == null || properties == null || properties.isEmpty()) {
            return value;
        }

        String current = value;
        for (int iteration = 0; iteration < 20; iteration++) {
            StringBuilder buffer = new StringBuilder();
            int position = 0;
            boolean changed = false;

            while (position < current.length()) {
                int start = current.indexOf("${", position);
                if (start < 0) {
                    buffer.append(current.substring(position));
                    break;
                }

                int end = current.indexOf('}', start + 2);
                if (end < 0) {
                    buffer.append(current.substring(position));
                    break;
                }

                buffer.append(current, position, start);
                String key = current.substring(start + 2, end);
                String replacement = properties.get(key);

                if (replacement != null) {
                    buffer.append(replacement);
                    changed = true;
                } else {
                    buffer.append("${").append(key).append("}");
                }
                position = end + 1;
            }

            if (!changed) {
                break;
            }
            current = buffer.toString();
        }

        return current;
    }

    private void putIfNotEmpty(
            Map<String, String> properties,
            String key,
            String value
    ) {
        if (value != null && !value.isBlank()) {
            properties.put(key, value);
        }
    }

    private String firstNonEmpty(String... values) {
        return Arrays.stream(values)
                .map(this::normalize)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
