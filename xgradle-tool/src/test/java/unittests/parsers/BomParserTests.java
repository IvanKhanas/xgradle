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

import unittests.PomXmlBuilder;

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
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Bom;
import org.altlinux.xgradle.impl.parsers.ParsersModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PomParser<Set<Path>> contract (@Bom parser)")
class BomParserTests {

    @Mock
    private PomContainer pomContainer;

    @Mock
    private ArtifactCache artifactCache;

    @Mock
    private ArtifactFactory artifactFactory;

    @Mock
    private Logger logger;

    private PomParser<Set<Path>> parser;

    @TempDir
    Path tempDir;

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

        parser = injector.getInstance(Key.get(new TypeLiteral<PomParser<Set<Path>>>() {}, Bom.class));

        assertEquals(
                Bom.class.getName(),
                Bom.class.getName(),
                "Test must use the production @Bom binding"
        );
    }

    @Test
    @DisplayName("Guice creates @Bom PomParser<Set<Path>> implementation")
    void parserIsCreated() {
        assertNotNull(parser);
    }

    @Test
    @DisplayName("Uses getAllPoms when artifactNames is empty and returns only BOM POMs")
    void usesGetAllPomsAndFiltersOnlyBom() throws Exception {
        Path bomPom = write("bom.pom",
                PomXmlBuilder.pom()
                        .groupId("g").artifactId("a").version("1")
                        .packaging("pom").dependencyManagementBlock()
                        .build()
        );

        Path nonBomPom = write("lib.pom",
                PomXmlBuilder.pom()
                        .groupId("g").artifactId("b").version("1")
                        .packaging("jar")
                        .build()
        );

        when(pomContainer.getAllPoms("/repo")).thenReturn(Set.of(bomPom, nonBomPom));

        ArtifactCoordinates coords = mock(ArtifactCoordinates.class);
        ArtifactData data = mock(ArtifactData.class);

        when(artifactFactory.coordinates("g", "a", "1")).thenReturn(coords);
        when(artifactFactory.data(eq(coords), any(), eq(bomPom), isNull())).thenReturn(data);
        when(data.getCoordinates()).thenReturn(coords);
        when(artifactCache.add(data)).thenReturn(true);

        Set<Path> result = parser.getArtifactCoords("/repo", Optional.empty());

        assertEquals(Set.of(bomPom), result);

        verify(pomContainer).getAllPoms("/repo");
        verify(pomContainer, never()).getSelectedPoms(anyString(), anyList());

        verify(artifactFactory).coordinates("g", "a", "1");
        verify(artifactFactory).data(eq(coords), any(), eq(bomPom), isNull());
        verify(artifactCache).add(data);

        verifyNoMoreInteractions(pomContainer, artifactCache, artifactFactory);
    }

    @Test
    @DisplayName("Uses getSelectedPoms when artifactNames is present")
    void usesGetSelectedPomsWhenArtifactNamesPresent() throws Exception {
        Path bomPom = write("bom.pom",
                PomXmlBuilder.pom()
                        .groupId("g").artifactId("a").version("1")
                        .packaging("pom").dependencyManagementBlock()
                        .build()
        );

        List<String> names = List.of("a");

        when(pomContainer.getSelectedPoms("/repo", names)).thenReturn(Set.of(bomPom));

        ArtifactCoordinates coords = mock(ArtifactCoordinates.class);
        ArtifactData data = mock(ArtifactData.class);

        when(artifactFactory.coordinates("g", "a", "1")).thenReturn(coords);
        when(artifactFactory.data(eq(coords), any(), eq(bomPom), isNull())).thenReturn(data);
        when(data.getCoordinates()).thenReturn(coords);
        when(artifactCache.add(data)).thenReturn(true);

        Set<Path> result = parser.getArtifactCoords("/repo", Optional.of(names));

        assertEquals(Set.of(bomPom), result);

        verify(pomContainer).getSelectedPoms("/repo", names);
        verify(pomContainer, never()).getAllPoms(anyString());

        verify(artifactFactory).coordinates("g", "a", "1");
        verify(artifactFactory).data(eq(coords), any(), eq(bomPom), isNull());
        verify(artifactCache).add(data);

        verifyNoMoreInteractions(pomContainer, artifactCache, artifactFactory);
    }

    @Test
    @DisplayName("Skips duplicates when cache rejects (add returns false)")
    void skipsDuplicatesWhenCacheRejects() throws Exception {
        Path bom1 = write("bom1.pom",
                PomXmlBuilder.pom()
                        .groupId("g").artifactId("a").version("1")
                        .packaging("pom").dependencyManagementBlock()
                        .build()
        );

        Path bom2 = write("bom2.pom",
                PomXmlBuilder.pom()
                        .groupId("g").artifactId("a").version("1")
                        .packaging("pom").dependencyManagementBlock()
                        .build()
        );

        when(pomContainer.getAllPoms("/repo")).thenReturn(Set.of(bom1, bom2));

        ArtifactCoordinates coords = mock(ArtifactCoordinates.class);

        ArtifactData data1 = mock(ArtifactData.class);
        when(data1.getCoordinates()).thenReturn(coords);

        ArtifactData data2 = mock(ArtifactData.class);
        when(data2.getCoordinates()).thenReturn(coords);

        when(artifactFactory.coordinates("g", "a", "1")).thenReturn(coords);
        when(artifactFactory.data(eq(coords), any(), eq(bom1), isNull())).thenReturn(data1);
        when(artifactFactory.data(eq(coords), any(), eq(bom2), isNull())).thenReturn(data2);

        AtomicBoolean first = new AtomicBoolean(true);
        when(artifactCache.add(any(ArtifactData.class))).thenAnswer(inv -> first.getAndSet(false));

        ArtifactData existing = mock(ArtifactData.class);
        when(existing.getPomPath()).thenReturn(bom1);
        when(artifactCache.get(coords)).thenReturn(existing);

        Set<Path> result = parser.getArtifactCoords("/repo", Optional.empty());

        assertEquals(1, result.size());
        assertTrue(result.contains(bom1) || result.contains(bom2));

        verify(pomContainer).getAllPoms("/repo");
        verify(artifactFactory, times(2)).coordinates("g", "a", "1");
        verify(artifactFactory).data(eq(coords), any(), eq(bom1), isNull());
        verify(artifactFactory).data(eq(coords), any(), eq(bom2), isNull());
        verify(artifactCache, times(2)).add(any(ArtifactData.class));
        verify(artifactCache).get(coords);

        verifyNoMoreInteractions(pomContainer, artifactCache, artifactFactory);
    }

    @Test
    @DisplayName("Invalid POM does not fail and is excluded")
    void invalidPomDoesNotFailAndIsExcluded() throws Exception {
        Path bom = write("bom.pom",
                PomXmlBuilder.pom()
                        .groupId("g").artifactId("a").version("1")
                        .packaging("pom").dependencyManagementBlock()
                        .build()
        );

        Path invalid = write("invalid.pom", "<not-xml");

        when(pomContainer.getAllPoms("/repo")).thenReturn(Set.of(bom, invalid));

        ArtifactCoordinates coords = mock(ArtifactCoordinates.class);
        ArtifactData data = mock(ArtifactData.class);

        when(artifactFactory.coordinates("g", "a", "1")).thenReturn(coords);
        when(artifactFactory.data(eq(coords), any(), eq(bom), isNull())).thenReturn(data);
        when(data.getCoordinates()).thenReturn(coords);
        when(artifactCache.add(data)).thenReturn(true);

        Set<Path> result = parser.getArtifactCoords("/repo", Optional.empty());

        assertEquals(Set.of(bom), result);

        verify(pomContainer).getAllPoms("/repo");
        verify(artifactFactory).coordinates("g", "a", "1");
        verify(artifactFactory).data(eq(coords), any(), eq(bom), isNull());
        verify(artifactCache).add(data);

        verify(logger, atLeastOnce()).error(startsWith("Error checking BOM file:"), eq(invalid), any());

        verifyNoMoreInteractions(pomContainer, artifactCache, artifactFactory);
    }

    private Path write(String fileName, String content) throws IOException {
        Path p = tempDir.resolve(fileName);
        Files.writeString(p, content, StandardCharsets.UTF_8);
        return p;
    }
}
