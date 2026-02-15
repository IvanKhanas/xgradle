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
package unittests.parsers;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;

import org.altlinux.xgradle.interfaces.caches.ArtifactCache;
import org.altlinux.xgradle.interfaces.containers.PomContainer;
import org.altlinux.xgradle.interfaces.model.ArtifactCoordinates;
import org.altlinux.xgradle.interfaces.model.ArtifactData;
import org.altlinux.xgradle.interfaces.model.ArtifactFactory;
import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Library;
import org.altlinux.xgradle.impl.parsers.ParsersModule;

import org.apache.maven.model.Model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import unittests.PomXmlBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PomParser<HashMap<String,Path>> contract (Library parser)")
class LibraryPomParserTests {

    @Mock
    private PomContainer pomContainer;

    @Mock
    private ArtifactCache artifactCache;

    @Mock
    private ArtifactFactory artifactFactory;

    @Mock
    private Logger logger;

    private PomParser<HashMap<String, Path>> parser;

    @BeforeEach
    void setUp() {
        Injector injector = Guice.createInjector(
                Modules.override(new ParsersModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(PomContainer.class).toInstance(pomContainer);
                        bind(ArtifactCache.class).toInstance(artifactCache);
                        bind(ArtifactFactory.class).toInstance(artifactFactory);
                        bind(Logger.class).toInstance(logger);
                    }
                })
        );

        parser = injector.getInstance(
                Key.get(new TypeLiteral<PomParser<HashMap<String, Path>>>() {}, Library.class)
        );

        when(artifactFactory.coordinates(nullable(String.class), nullable(String.class), nullable(String.class)))
                .thenAnswer(inv -> {
                    String g = inv.getArgument(0, String.class);
                    String a = inv.getArgument(1, String.class);
                    String v = inv.getArgument(2, String.class);

                    ArtifactCoordinates coords = mock(ArtifactCoordinates.class);
                    when(coords.getGroupId()).thenReturn(g);
                    when(coords.getArtifactId()).thenReturn(a);
                    when(coords.getVersion()).thenReturn(v);
                    return coords;
                });

        when(artifactFactory.data(any(ArtifactCoordinates.class), any(Model.class), any(Path.class), any(Path.class)))
                .thenAnswer(inv -> {
                    ArtifactCoordinates coords = inv.getArgument(0);
                    Path pomPath = inv.getArgument(2);
                    Path jarPath = inv.getArgument(3);

                    ArtifactData data = mock(ArtifactData.class);
                    when(data.getCoordinates()).thenReturn(coords);
                    when(data.getPomPath()).thenReturn(pomPath);
                    when(data.getJarPath()).thenReturn(jarPath);
                    return data;
                });
    }

    @Test
    @DisplayName("whenArtifactNamesPresentUsesGetSelectedPoms")
    void whenArtifactNamesPresentUsesGetSelectedPoms(@TempDir Path dir) throws Exception {
        Path pom = writePom(
                dir.resolve("lib-1.0.pom"),
                PomXmlBuilder.pom()
                        .groupId("org.example")
                        .artifactId("lib")
                        .version("1.0")
                        .build()
        );
        Path jar = touch(dir.resolve("lib-1.0.jar"));

        List<String> names = List.of("lib");
        when(pomContainer.getSelectedPoms(dir.toString(), names)).thenReturn(Set.of(pom));
        when(artifactCache.add(any(ArtifactData.class))).thenReturn(true);

        HashMap<String, Path> result = parser.getArtifactCoords(dir.toString(), Optional.of(names));

        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals(jar, result.get(pom.toString()))
        );

        verify(pomContainer).getSelectedPoms(dir.toString(), names);
        verify(pomContainer, never()).getAllPoms(anyString());

        verify(artifactCache).add(any(ArtifactData.class));
        verifyNoMoreInteractions(pomContainer, artifactCache);
    }

    @Test
    @DisplayName("whenArtifactNamesEmptyUsesGetAllPoms")
    void whenArtifactNamesEmptyUsesGetAllPoms(@TempDir Path dir) throws Exception {
        Path pom = writePom(
                dir.resolve("lib-1.0.pom"),
                PomXmlBuilder.pom()
                        .groupId("org.example")
                        .artifactId("lib")
                        .version("1.0")
                        .build()
        );
        Path jar = touch(dir.resolve("lib-1.0.jar"));

        when(pomContainer.getAllPoms(dir.toString())).thenReturn(Set.of(pom));
        when(artifactCache.add(any(ArtifactData.class))).thenReturn(true);

        HashMap<String, Path> result = parser.getArtifactCoords(dir.toString(), Optional.empty());

        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals(jar, result.get(pom.toString()))
        );

        verify(pomContainer).getAllPoms(dir.toString());
        verify(pomContainer, never()).getSelectedPoms(anyString(), anyList());

        verify(artifactCache).add(any(ArtifactData.class));
        verifyNoMoreInteractions(pomContainer, artifactCache);
    }

    @Test
    @DisplayName("fallsBackToParentForGroupIdAndVersion")
    void fallsBackToParentForGroupIdAndVersion(@TempDir Path dir) throws Exception {
        Path pom = writePom(
                dir.resolve("child.pom"),
                PomXmlBuilder.pom()
                        .parent("org.parent", "parent", "2.5")
                        .artifactId("child")
                        .build()
        );
        Path jar = touch(dir.resolve("child-2.5.jar"));

        when(pomContainer.getAllPoms(dir.toString())).thenReturn(Set.of(pom));
        when(artifactCache.add(any(ArtifactData.class))).thenReturn(true);

        HashMap<String, Path> result = parser.getArtifactCoords(dir.toString(), Optional.empty());

        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals(jar, result.get(pom.toString()))
        );

        verify(pomContainer).getAllPoms(dir.toString());
        verify(artifactCache).add(any(ArtifactData.class));
        verifyNoMoreInteractions(pomContainer, artifactCache);
    }

    @Test
    @DisplayName("skipsDuplicateWhenSameCoordinatesSeenTwice")
    void skipsDuplicateWhenSameCoordinatesSeenTwice(@TempDir Path dir) throws Exception {
        Path aPom = writePom(
                dir.resolve("a.pom"),
                PomXmlBuilder.pom()
                        .groupId("org.example")
                        .artifactId("dup")
                        .version("1.0")
                        .build()
        );
        Path bPom = writePom(
                dir.resolve("b.pom"),
                PomXmlBuilder.pom()
                        .groupId("org.example")
                        .artifactId("dup")
                        .version("1.0")
                        .build()
        );
        Path jar = touch(dir.resolve("dup-1.0.jar"));

        when(pomContainer.getAllPoms(dir.toString())).thenReturn(Set.of(aPom, bPom));

        AtomicBoolean first = new AtomicBoolean(true);
        when(artifactCache.add(any(ArtifactData.class)))
                .thenAnswer(inv -> first.getAndSet(false));

        ArtifactData existing = mock(ArtifactData.class);
        Path existingPom = Path.of("/tmp/existing.pom");
        when(existing.getPomPath()).thenReturn(existingPom);
        when(artifactCache.get(any(ArtifactCoordinates.class))).thenReturn(existing);

        HashMap<String, Path> result = parser.getArtifactCoords(dir.toString(), Optional.empty());

        assertEquals(1, result.size());

        String onlyKey = result.keySet().iterator().next();
        assertTrue(onlyKey.equals(aPom.toString()) || onlyKey.equals(bPom.toString()));
        assertEquals(jar, result.get(onlyKey));

        verify(artifactCache, times(2)).add(any(ArtifactData.class));
        verify(artifactCache, times(1)).get(any(ArtifactCoordinates.class));

        verify(logger, atLeastOnce()).warn(
                eq("Skipping duplicate artifact: {} (already processed from: {})"),
                any(ArtifactCoordinates.class),
                eq(existingPom)
        );
    }

    private static Path touch(Path p) throws IOException {
        if (Files.notExists(p)) {
            Files.createFile(p);
        }
        return p;
    }

    private static Path writePom(Path pomPath, String xml) throws IOException {
        Files.write(pomPath, xml.getBytes(StandardCharsets.UTF_8));
        return pomPath;
    }
}
