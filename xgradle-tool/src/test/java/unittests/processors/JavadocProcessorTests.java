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
package unittests.processors;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;

import org.altlinux.xgradle.interfaces.model.ArtifactCoordinates;
import org.altlinux.xgradle.interfaces.model.ArtifactFactory;
import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.altlinux.xgradle.interfaces.processors.PomProcessor;
import org.altlinux.xgradle.interfaces.services.PomService;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Bom;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.GradlePlugin;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Javadoc;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Library;
import org.altlinux.xgradle.impl.config.ToolConfig;
import org.altlinux.xgradle.impl.processors.ProcessorsModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("@Javadoc PomProcessor<HashMap<String,Path>> (DefaultJavadocProcessor)")
class JavadocProcessorTests {

    @Mock private PomParser<HashMap<String, Path>> javadocParser;
    @Mock private PomService pomService;
    @Mock private ToolConfig toolConfig;
    @Mock private ArtifactFactory artifactFactory;
    @Mock private Logger logger;

    @Mock private PomParser<HashMap<String, Path>> libraryParserDummy;
    @Mock private PomParser<HashMap<String, Path>> gradlePluginParserDummy;
    @Mock private PomParser<Set<Path>> bomParserDummy;

    private PomProcessor<HashMap<String, Path>> processor;

    @BeforeEach
    void setUp() {
        lenient().when(artifactFactory.coordinates(anyString(), anyString(), anyString()))
                .thenAnswer(inv -> new SimpleCoords(
                        inv.getArgument(0, String.class),
                        inv.getArgument(1, String.class),
                        inv.getArgument(2, String.class)
                ));

        Injector injector = Guice.createInjector(
                Modules.override(new ProcessorsModule())
                        .with(new AbstractModule() {
                            @Override
                            protected void configure() {
                                bind(Key.get(new TypeLiteral<PomParser<HashMap<String, Path>>>() {}, Javadoc.class))
                                        .toInstance(javadocParser);

                                bind(Key.get(new TypeLiteral<PomParser<HashMap<String, Path>>>() {}, Library.class))
                                        .toInstance(libraryParserDummy);
                                bind(Key.get(new TypeLiteral<PomParser<HashMap<String, Path>>>() {}, GradlePlugin.class))
                                        .toInstance(gradlePluginParserDummy);
                                bind(Key.get(new TypeLiteral<PomParser<Set<Path>>>() {}, Bom.class))
                                        .toInstance(bomParserDummy);

                                bind(PomService.class).toInstance(pomService);
                                bind(ToolConfig.class).toInstance(toolConfig);
                                bind(ArtifactFactory.class).toInstance(artifactFactory);
                                bind(Logger.class).toInstance(logger);
                            }
                        })
        );

        processor = injector.getInstance(
                Key.get(new TypeLiteral<PomProcessor<HashMap<String, Path>>>() {}, Javadoc.class)
        );

        assertEquals(
                "org.altlinux.xgradle.impl.processors.DefaultJavadocProcessor",
                processor.getClass().getName()
        );
    }

    @Test
    @DisplayName("Filters duplicates by groupId:artifactId before excludeArtifacts; snapshots allowed")
    void filtersDuplicatesBeforeExcludeArtifacts(@TempDir Path tmp) throws Exception {
        Path pom1 = write(tmp.resolve("lib-1.pom"), pomXml("org.example", "lib", "1"));
        Path pom2 = write(tmp.resolve("lib-2.pom"), pomXml("org.example", "lib", "2"));

        Path jd1 = tmp.resolve("lib-1-javadoc.jar");
        Path jd2 = tmp.resolve("lib-2-javadoc.jar");

        HashMap<String, Path> parsed = new HashMap<>();
        parsed.put(pom1.toString(), jd1);
        parsed.put(pom2.toString(), jd2);

        when(javadocParser.getArtifactCoords(eq(tmp.toString()), any())).thenReturn(parsed);

        when(toolConfig.getExcludedArtifacts()).thenReturn(List.of());
        when(toolConfig.isAllowSnapshots()).thenReturn(true);

        when(pomService.excludeArtifacts(eq(List.of()), ArgumentMatchers.<HashMap<String, Path>>any()))
                .thenAnswer(inv -> inv.getArgument(1));

        HashMap<String, Path> result = processor.pomsFromDirectory(tmp.toString(), Optional.empty());

        assertEquals(1, result.size());
        assertTrue(result.containsKey(pom1.toString()) || result.containsKey(pom2.toString()));

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<HashMap<String, Path>> cap = (ArgumentCaptor) ArgumentCaptor.forClass(HashMap.class);
        verify(pomService).excludeArtifacts(eq(List.of()), cap.capture());
        assertEquals(1, cap.getValue().size());

        InOrder inOrder = inOrder(javadocParser, toolConfig, pomService);
        inOrder.verify(javadocParser).getArtifactCoords(eq(tmp.toString()), eq(Optional.empty()));
        inOrder.verify(toolConfig).getExcludedArtifacts();
        inOrder.verify(pomService).excludeArtifacts(eq(List.of()), ArgumentMatchers.<HashMap<String, Path>>any());
        inOrder.verify(toolConfig).isAllowSnapshots();

        verify(pomService, never()).excludeSnapshots(ArgumentMatchers.<HashMap<String, Path>>any());
        verifyNoMoreInteractions(javadocParser, toolConfig, pomService);
    }

    @Test
    @DisplayName("Invalid XML POM => kept and warn logged")
    void keepsArtifactWhenPomIsInvalid(@TempDir Path tmp) throws Exception {
        Path badPom = write(tmp.resolve("bad.pom"), "<project><not-xml");
        Path jd = tmp.resolve("bad-javadoc.jar");

        HashMap<String, Path> parsed = new HashMap<>();
        parsed.put(badPom.toString(), jd);

        when(javadocParser.getArtifactCoords(eq(tmp.toString()), any())).thenReturn(parsed);

        when(toolConfig.getExcludedArtifacts()).thenReturn(List.of());
        when(toolConfig.isAllowSnapshots()).thenReturn(true);

        when(pomService.excludeArtifacts(eq(List.of()), ArgumentMatchers.<HashMap<String, Path>>any()))
                .thenAnswer(inv -> inv.getArgument(1));

        HashMap<String, Path> result = processor.pomsFromDirectory(tmp.toString(), Optional.empty());

        assertEquals(1, result.size());
        assertEquals(jd, result.get(badPom.toString()));

        verify(logger, atLeastOnce()).warn(
                eq("Failed to extract coordinates from POM: {}, adding artifact anyway"),
                eq(Path.of(badPom.toString()))
        );

        verify(pomService, never()).excludeSnapshots(ArgumentMatchers.<HashMap<String, Path>>any());
    }

    @Test
    @DisplayName("allowSnapshots=false => excludeSnapshots is called")
    void appliesSnapshotFilteringWhenDisabled(@TempDir Path tmp) throws Exception {
        Path pom = write(tmp.resolve("lib.pom"), pomXml("org.example", "lib", "1-SNAPSHOT"));
        Path jd = tmp.resolve("lib-1-SNAPSHOT-javadoc.jar");

        HashMap<String, Path> parsed = new HashMap<>();
        parsed.put(pom.toString(), jd);

        when(javadocParser.getArtifactCoords(eq(tmp.toString()), any())).thenReturn(parsed);

        when(toolConfig.getExcludedArtifacts()).thenReturn(List.of());
        when(toolConfig.isAllowSnapshots()).thenReturn(false);

        when(pomService.excludeArtifacts(eq(List.of()), ArgumentMatchers.<HashMap<String, Path>>any()))
                .thenAnswer(inv -> inv.getArgument(1));

        HashMap<String, Path> afterSnapshots = new HashMap<>();
        when(pomService.excludeSnapshots(ArgumentMatchers.<HashMap<String, Path>>any())).thenReturn(afterSnapshots);

        HashMap<String, Path> result = processor.pomsFromDirectory(tmp.toString(), Optional.empty());

        assertSame(afterSnapshots, result);

        InOrder inOrder = inOrder(javadocParser, toolConfig, pomService);
        inOrder.verify(javadocParser).getArtifactCoords(eq(tmp.toString()), eq(Optional.empty()));
        inOrder.verify(toolConfig).getExcludedArtifacts();
        inOrder.verify(pomService).excludeArtifacts(eq(List.of()), ArgumentMatchers.<HashMap<String, Path>>any());
        inOrder.verify(toolConfig).isAllowSnapshots();
        inOrder.verify(pomService).excludeSnapshots(ArgumentMatchers.<HashMap<String, Path>>any());

        verifyNoMoreInteractions(javadocParser, toolConfig, pomService);
    }

    private static Path write(Path path, String content) throws IOException {
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        return path;
    }

    private static String pomXml(String groupId, String artifactId, String version) {
        return ""
                + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">"
                + "<modelVersion>4.0.0</modelVersion>"
                + "<groupId>" + groupId + "</groupId>"
                + "<artifactId>" + artifactId + "</artifactId>"
                + "<version>" + version + "</version>"
                + "</project>";
    }

    private static final class SimpleCoords implements ArtifactCoordinates {
        private final String groupId;
        private final String artifactId;
        private final String version;

        private SimpleCoords(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        @Override public String getGroupId() { return groupId; }
        @Override public String getArtifactId() { return artifactId; }
        @Override public String getVersion() { return version; }
    }
}
