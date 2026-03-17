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
package unittests.collectors;

import org.altlinux.xgradle.impl.collectors.DefaultSbomComponentCollector;
import org.altlinux.xgradle.impl.enums.SbomComponentKind;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.models.SbomComponent;
import org.altlinux.xgradle.interfaces.resolution.ResolvedArtifactsRegistry;
import org.altlinux.xgradle.interfaces.services.PomMetadata;
import org.altlinux.xgradle.interfaces.services.PomMetadataLicense;
import org.altlinux.xgradle.interfaces.services.PomMetadataReader;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultSbomComponentCollector contract")
class DefaultSbomComponentCollectorTests {

    @Mock
    private PomMetadataReader pomMetadataReader;

    @Test
    @DisplayName("Collects library plugin and resolved jar components")
    void collectsLibraryPluginAndResolvedJarComponents(@TempDir Path tempDir) throws Exception {
        Project root = ProjectBuilder.builder().withName("root").build();
        Set<File> resolvedJars = ResolvedArtifactsRegistry.getOrCreate(root);
        Path resolvedJar = Files.createFile(tempDir.resolve("resolved-extra.jar"));
        resolvedJars.add(resolvedJar.toFile());

        Path pomPath = Files.createFile(tempDir.resolve("artifact.pom"));
        when(pomMetadataReader.read(pomPath)).thenReturn(new PomMetadata(
                "https://project.example",
                "https://scm.example",
                List.of(new PomMetadataLicense(" Apache-2.0 ", " https://apache.org "))
        ));

        MavenCoordinate library = coordinate(
                "org.example",
                "core-lib",
                "1.0.0",
                "jar",
                pomPath
        );
        MavenCoordinate plugin = coordinate(
                "org.example",
                "gradle-plugin",
                "1.0.0",
                "jar",
                pomPath
        );
        MavenCoordinate bom = coordinate(
                "org.example",
                "core-bom",
                "1.0.0",
                "pom",
                pomPath
        );

        DefaultSbomComponentCollector collector = new DefaultSbomComponentCollector(pomMetadataReader);
        List<SbomComponent> components = collector.collect(
                root,
                List.of(library, bom),
                List.of(plugin)
        );

        assertEquals(3, components.size());

        SbomComponent libraryComponent = components.stream()
                .filter(component -> "core-lib".equals(component.getArtifactId()))
                .findFirst()
                .orElseThrow();
        SbomComponent pluginComponent = components.stream()
                .filter(component -> "gradle-plugin".equals(component.getArtifactId()))
                .findFirst()
                .orElseThrow();
        SbomComponent fileComponent = components.stream()
                .filter(component -> component.getComponentKind() == SbomComponentKind.FILE)
                .findFirst()
                .orElseThrow();

        assertAll(
                () -> assertEquals(SbomComponentKind.LIBRARY, libraryComponent.getComponentKind()),
                () -> assertEquals("https://project.example", libraryComponent.getProjectUrl()),
                () -> assertEquals("https://scm.example", libraryComponent.getScmUrl()),
                () -> assertEquals(1, libraryComponent.getLicenses().size()),
                () -> assertEquals("Apache-2.0", libraryComponent.getLicenses().get(0).getName()),
                () -> assertEquals(SbomComponentKind.GRADLE_PLUGIN, pluginComponent.getComponentKind()),
                () -> assertEquals("resolved-extra.jar", fileComponent.getFileName())
        );

        verify(pomMetadataReader, times(1)).read(pomPath);
    }

    @Test
    @DisplayName("Skips ineligible coordinates and handles missing pom metadata")
    void skipsIneligibleCoordinatesAndHandlesMissingPomMetadata() {
        Project root = ProjectBuilder.builder().withName("root").build();

        MavenCoordinate missingGroup = MavenCoordinate.builder()
                .artifactId("broken")
                .version("1.0.0")
                .packaging("jar")
                .build();
        MavenCoordinate noPomPath = coordinate(
                "org.example",
                "without-pom",
                "1.0.0",
                "jar",
                null
        );

        DefaultSbomComponentCollector collector = new DefaultSbomComponentCollector(pomMetadataReader);
        List<SbomComponent> components = collector.collect(
                root,
                List.of(missingGroup, noPomPath),
                null
        );

        assertAll(
                () -> assertEquals(1, components.size()),
                () -> assertEquals("without-pom", components.get(0).getArtifactId()),
                () -> assertTrue(components.get(0).getLicenses().isEmpty())
        );
        verifyNoInteractions(pomMetadataReader);
    }

    private MavenCoordinate coordinate(
            String groupId,
            String artifactId,
            String version,
            String packaging,
            Path pomPath
    ) {
        return MavenCoordinate.builder()
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .packaging(packaging)
                .pomPath(pomPath)
                .build();
    }
}
