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

// src/test/java/unittests/processors/BomProcessorTests.java
package unittests.processors;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;

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
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("@Bom PomProcessor<Set<Path>> (DefaultBomProcessor)")
class BomProcessorTests {

    @Mock
    private PomParser<Set<Path>> bomParser;

    @Mock
    private PomService pomService;

    @Mock
    private ToolConfig toolConfig;

    @Mock
    private PomParser<HashMap<String, Path>> libraryParserDummy;

    @Mock
    private PomParser<HashMap<String, Path>> gradlePluginParserDummy;

    @Mock
    private PomParser<HashMap<String, Path>> javadocParserDummy;

    @Mock
    private ArtifactFactory artifactFactory;

    @Mock
    private Logger logger;

    private PomProcessor<Set<Path>> processor;

    @BeforeEach
    void setUp() {
        Injector injector = Guice.createInjector(
                Modules.override(new ProcessorsModule())
                        .with(new AbstractModule() {
                            @Override
                            protected void configure() {
                                bind(Key.get(new TypeLiteral<PomParser<Set<Path>>>() {}, Bom.class))
                                        .toInstance(bomParser);

                                bind(Key.get(new TypeLiteral<PomParser<HashMap<String, Path>>>() {}, Library.class))
                                        .toInstance(libraryParserDummy);
                                bind(Key.get(new TypeLiteral<PomParser<HashMap<String, Path>>>() {}, GradlePlugin.class))
                                        .toInstance(gradlePluginParserDummy);
                                bind(Key.get(new TypeLiteral<PomParser<HashMap<String, Path>>>() {}, Javadoc.class))
                                        .toInstance(javadocParserDummy);

                                bind(PomService.class).toInstance(pomService);
                                bind(ToolConfig.class).toInstance(toolConfig);
                                bind(ArtifactFactory.class).toInstance(artifactFactory);
                                bind(Logger.class).toInstance(logger);
                            }
                        })
        );

        processor = injector.getInstance(
                Key.get(new TypeLiteral<PomProcessor<Set<Path>>>() {}, Bom.class)
        );

        assertEquals(
                "org.altlinux.xgradle.impl.processors.DefaultBomProcessor",
                processor.getClass().getName()
        );
    }

    @Test
    @DisplayName("Delegates to parser; applies excludeArtifacts -> removeParentBlocks -> excludeSnapshots (when allowSnapshots=false)")
    void processesAndFiltersInOrder() {
        Optional<List<String>> names = Optional.of(List.of("bom"));

        Set<Path> parsed = Set.of(Path.of("/repo/bom.pom"));
        Set<Path> afterExclude = Set.of(Path.of("/repo/bom.pom"));
        Set<Path> afterSnapshots = Set.of(Path.of("/repo/bom.pom"));

        when(bomParser.getArtifactCoords("/repo", names)).thenReturn(parsed);

        when(toolConfig.getExcludedArtifacts()).thenReturn(List.of("x"));
        when(toolConfig.getRemoveParentPoms()).thenReturn(List.of("parent-a"));
        when(toolConfig.isAllowSnapshots()).thenReturn(false);

        when(pomService.excludeArtifacts(List.of("x"), parsed)).thenReturn(afterExclude);
        when(pomService.excludeSnapshots(afterExclude)).thenReturn(afterSnapshots);

        Set<Path> result = processor.pomsFromDirectory("/repo", names);
        assertSame(afterSnapshots, result);

        InOrder inOrder = inOrder(bomParser, pomService, toolConfig);
        inOrder.verify(bomParser).getArtifactCoords("/repo", names);
        inOrder.verify(toolConfig).getExcludedArtifacts();
        inOrder.verify(pomService).excludeArtifacts(List.of("x"), parsed);
        inOrder.verify(toolConfig).getRemoveParentPoms();
        inOrder.verify(pomService).removeParentBlocks(afterExclude, List.of("parent-a"));
        inOrder.verify(toolConfig).isAllowSnapshots();
        inOrder.verify(pomService).excludeSnapshots(afterExclude);

        verifyNoMoreInteractions(bomParser, pomService, toolConfig);
    }

    @Test
    @DisplayName("When allowSnapshots=true: still calls removeParentBlocks, but does not call excludeSnapshots")
    void snapshotsAllowed() {
        Set<Path> parsed = Set.of(Path.of("/repo/bom.pom"));

        when(bomParser.getArtifactCoords("/repo", Optional.empty())).thenReturn(parsed);

        when(toolConfig.getExcludedArtifacts()).thenReturn(List.of());
        when(toolConfig.getRemoveParentPoms()).thenReturn(List.of());
        when(toolConfig.isAllowSnapshots()).thenReturn(true);

        when(pomService.excludeArtifacts(List.of(), parsed)).thenReturn(parsed);

        Set<Path> result = processor.pomsFromDirectory("/repo", Optional.empty());
        assertSame(parsed, result);

        InOrder inOrder = inOrder(bomParser, pomService, toolConfig);
        inOrder.verify(bomParser).getArtifactCoords("/repo", Optional.empty());
        inOrder.verify(toolConfig).getExcludedArtifacts();
        inOrder.verify(pomService).excludeArtifacts(List.of(), parsed);
        inOrder.verify(toolConfig).getRemoveParentPoms();
        inOrder.verify(pomService).removeParentBlocks(parsed, List.of());
        inOrder.verify(toolConfig).isAllowSnapshots();

        verify(pomService, never()).excludeSnapshots((Set<Path>) any());
        verifyNoMoreInteractions(bomParser, pomService, toolConfig);
    }
}
