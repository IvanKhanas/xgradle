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
package unittests.indexing;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

import org.altlinux.xgradle.interfaces.collectors.PomFilesCollector;
import org.altlinux.xgradle.interfaces.indexing.PomIndex;
import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.altlinux.xgradle.impl.indexing.IndexingModule;
import org.altlinux.xgradle.impl.model.MavenCoordinate;

import org.gradle.api.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PomIndex contract")
class PomIndexTests {

    @Mock
    private PomFilesCollector pomFilesCollector;

    @Mock
    private PomParser pomParser;

    @Mock
    private Logger logger;

    private PomIndex pomIndex;

    @BeforeEach
    void setUp() {
        Injector injector = Guice.createInjector(
                Modules.override(new IndexingModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(PomFilesCollector.class).toInstance(pomFilesCollector);
                        bind(PomParser.class).toInstance(pomParser);
                        bind(Logger.class).toInstance(logger);
                    }
                })
        );

        pomIndex = injector.getInstance(PomIndex.class);
    }

    @Test
    @DisplayName("build(List) keeps newest GA and sorts group entries")
    void buildListKeepsNewestAndSortsGroup() {
        Path pomA1 = Path.of("/repo/a-1.pom");
        Path pomA2 = Path.of("/repo/a-2.pom");
        Path pomB1 = Path.of("/repo/b-1.pom");

        MavenCoordinate a1 = MavenCoordinate.builder()
                .groupId("g")
                .artifactId("a")
                .version("1.0")
                .build();

        MavenCoordinate a2 = MavenCoordinate.builder()
                .groupId("g")
                .artifactId("a")
                .version("2.0")
                .build();

        MavenCoordinate b1 = MavenCoordinate.builder()
                .groupId("g")
                .artifactId("b")
                .version("1.1")
                .build();

        when(pomParser.parsePom(pomA1)).thenReturn(a1);
        when(pomParser.parsePom(pomA2)).thenReturn(a2);
        when(pomParser.parsePom(pomB1)).thenReturn(b1);

        pomIndex.build(List.of(pomA1, pomA2, pomB1));

        Optional<MavenCoordinate> resolved = pomIndex.find("g", "a");
        assertTrue(resolved.isPresent());
        assertEquals("2.0", resolved.get().getVersion());

        List<MavenCoordinate> group = pomIndex.findAllForGroup("g");
        assertEquals(3, group.size());
        assertEquals("a", group.get(0).getArtifactId());
        assertEquals("1.0", group.get(0).getVersion());
        assertEquals("a", group.get(1).getArtifactId());
        assertEquals("2.0", group.get(1).getVersion());
        assertEquals("b", group.get(2).getArtifactId());
        assertEquals("1.1", group.get(2).getVersion());

        Map<String, MavenCoordinate> snapshot = pomIndex.snapshot();
        assertEquals(2, snapshot.size());
        assertEquals("2.0", snapshot.get("g:a").getVersion());
        assertEquals("1.1", snapshot.get("g:b").getVersion());
    }

    @Test
    @DisplayName("build(Path) uses collector and skips null coords")
    void buildPathUsesCollectorAndSkipsNull() {
        Path root = Path.of("/repo");
        Path pom = root.resolve("a.pom");
        Path brokenPom = root.resolve("broken.pom");

        MavenCoordinate a1 = MavenCoordinate.builder()
                .groupId("g")
                .artifactId("a")
                .version("1")
                .build();

        when(pomFilesCollector.collect(root)).thenReturn(List.of(pom, brokenPom));
        when(pomParser.parsePom(pom)).thenReturn(a1);
        when(pomParser.parsePom(brokenPom)).thenReturn(null);

        pomIndex.build(root);

        verify(pomFilesCollector).collect(root);
        verify(pomParser).parsePom(pom);
        verify(pomParser).parsePom(brokenPom);

        assertTrue(pomIndex.find("g", "a").isPresent());
        assertTrue(pomIndex.findAllForGroup("missing").isEmpty());
    }
}
