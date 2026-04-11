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
package org.altlinux.xgradle.impl.collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.models.SbomComponent;
import org.altlinux.xgradle.impl.models.SbomLicense;
import org.altlinux.xgradle.interfaces.collectors.SbomComponentCollector;
import org.altlinux.xgradle.interfaces.resolution.ResolvedArtifactsRegistry;
import org.altlinux.xgradle.interfaces.services.PomMetadata;
import org.altlinux.xgradle.interfaces.services.PomMetadataLicense;
import org.altlinux.xgradle.interfaces.services.PomMetadataReader;

import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Collects SBOM components from resolved Maven coordinates and resolved JAR files.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
public final class DefaultSbomComponentCollector implements SbomComponentCollector {

    private final PomMetadataReader pomMetadataReader;

    @Inject
    public DefaultSbomComponentCollector(PomMetadataReader pomMetadataReader) {
        this.pomMetadataReader = pomMetadataReader;
    }

    @Override
    public List<SbomComponent> collect(
            Project rootProject,
            Collection<MavenCoordinate> artifacts,
            Collection<MavenCoordinate> pluginArtifacts
    ) {
        LinkedHashMap<String, SbomComponent> components = new LinkedHashMap<>();
        Map<Path, PomMetadata> metadataByPomPath = new LinkedHashMap<>();

        appendLibraryComponents(artifacts, components, metadataByPomPath);
        appendPluginComponents(pluginArtifacts, components, metadataByPomPath);
        appendResolvedJarComponents(rootProject, components);

        return new ArrayList<>(components.values());
    }

    private void appendLibraryComponents(
            Collection<MavenCoordinate> artifacts,
            LinkedHashMap<String, SbomComponent> components,
            Map<Path, PomMetadata> metadataByPomPath
    ) {
        if (artifacts == null) {
            return;
        }

        artifacts.stream()
                .filter(this::isEligibleCoordinate)
                .filter(coordinate -> !components.containsKey(uniqueKey(coordinate)))
                .forEach(coordinate -> {
                    PomMetadata metadata = readPomMetadata(coordinate, metadataByPomPath);
                    SbomComponent component = SbomComponent.maven(
                            coordinate.getGroupId(),
                            coordinate.getArtifactId(),
                            coordinate.getVersion(),
                            metadata.getProjectUrl(),
                            metadata.getScmUrl(),
                            toSbomLicenses(metadata.getLicenses())
                    );
                    components.put(uniqueKey(coordinate), component);
                });
    }

    private void appendPluginComponents(
            Collection<MavenCoordinate> artifacts,
            LinkedHashMap<String, SbomComponent> components,
            Map<Path, PomMetadata> metadataByPomPath
    ) {
        if (artifacts == null) {
            return;
        }

        artifacts.stream()
                .filter(this::isEligibleCoordinate)
                .forEach(coordinate -> {
                    PomMetadata metadata = readPomMetadata(coordinate, metadataByPomPath);
                    SbomComponent component = SbomComponent.mavenPlugin(
                            coordinate.getGroupId(),
                            coordinate.getArtifactId(),
                            coordinate.getVersion(),
                            metadata.getProjectUrl(),
                            metadata.getScmUrl(),
                            toSbomLicenses(metadata.getLicenses())
                    );
                    components.put(uniqueKey(coordinate), component);
                });
    }

    private void appendResolvedJarComponents(
            Project rootProject,
            Map<String, SbomComponent> components
    ) {
        Set<File> resolvedJars = ResolvedArtifactsRegistry.get(rootProject);
        if (resolvedJars == null) {
            return;
        }

        resolvedJars.stream()
                .filter(jar -> jar != null && jar.isFile())
                .forEach(jar -> {
                    SbomComponent component = SbomComponent.file(jar.getName());
                    components.putIfAbsent(component.uniqueKey(), component);
                });
    }

    private boolean isEligibleCoordinate(MavenCoordinate coordinate) {
        if (coordinate == null || coordinate.isBom()) {
            return false;
        }

        if (coordinate.getGroupId() == null || coordinate.getArtifactId() == null) {
            return false;
        }

        return !"pom".equalsIgnoreCase(coordinate.getPackaging());
    }

    private String uniqueKey(MavenCoordinate coordinate) {
        String version = coordinate.getVersion();
        return coordinate.getGroupId()
                + ":"
                + coordinate.getArtifactId()
                + ":"
                + (version != null ? version : "");
    }

    private PomMetadata readPomMetadata(
            MavenCoordinate coordinate,
            Map<Path, PomMetadata> metadataByPomPath
    ) {
        Path pomPath = coordinate.getPomPath();
        if (pomPath == null) {
            return PomMetadata.empty();
        }

        PomMetadata cached = metadataByPomPath.get(pomPath);
        if (cached != null) {
            return cached;
        }

        PomMetadata metadata = pomMetadataReader.read(pomPath);
        PomMetadata normalized = metadata != null ? metadata : PomMetadata.empty();
        metadataByPomPath.put(pomPath, normalized);
        return normalized;
    }

    private List<SbomLicense> toSbomLicenses(List<PomMetadataLicense> metadataLicenses) {
        if (metadataLicenses == null || metadataLicenses.isEmpty()) {
            return List.of();
        }

        return metadataLicenses.stream()
                .filter(metadataLicense -> metadataLicense != null)
                .map(metadataLicense -> new SbomLicense(metadataLicense.getName(), metadataLicense.getUrl()))
                .collect(Collectors.toUnmodifiableList());
    }
}
