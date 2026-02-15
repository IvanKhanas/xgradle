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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;

import org.altlinux.xgradle.interfaces.collectors.ArtifactCollector;
import org.altlinux.xgradle.interfaces.processors.PomProcessor;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.GradlePlugin;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Javadoc;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Library;
import org.altlinux.xgradle.impl.collectors.CollectorsModule;
import org.altlinux.xgradle.impl.enums.ProcessingType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArtifactCollector contract")
class ArtifactCollectorTests {

    @Mock
    private PomProcessor<HashMap<String, Path>> libraryProcessor;

    @Mock
    private PomProcessor<HashMap<String, Path>> pluginProcessor;

    @Mock
    private PomProcessor<HashMap<String, Path>> javadocProcessor;

    private ArtifactCollector collector;

    @BeforeEach
    void setUp() {
        Injector injector = Guice.createInjector(
                Modules.override(new CollectorsModule())
                        .with(new AbstractModule() {
                            @Override
                            protected void configure() {
                                bind(Key.get(new TypeLiteral<PomProcessor<HashMap<String, Path>>>() {}, Library.class))
                                        .toInstance(libraryProcessor);

                                bind(Key.get(new TypeLiteral<PomProcessor<HashMap<String, Path>>>() {}, GradlePlugin.class))
                                        .toInstance(pluginProcessor);

                                bind(Key.get(new TypeLiteral<PomProcessor<HashMap<String, Path>>>() {}, Javadoc.class))
                                        .toInstance(javadocProcessor);
                            }
                        })
        );

        collector = injector.getInstance(ArtifactCollector.class);
    }

    /**
     * Ensures the collector is created via Guice and is not null.
     */
    @Test
    @DisplayName("Guice creates ArtifactCollector implementation")
    void collectorIsCreated() {
        assertNotNull(collector);
    }

    /**
     * Ensures that ProcessingType.LIBRARY is delegated to the @Library PomProcessor.
     */
    @Test
    @DisplayName("Delegates LIBRARY to @Library PomProcessor")
    void delegatesLibrary() {
        Optional<List<String>> names = Optional.of(List.of("a", "b"));
        HashMap<String, Path> expected = new HashMap<>();

        when(libraryProcessor.pomsFromDirectory("/repo", names)).thenReturn(expected);

        HashMap<String, Path> actual = collector.collect("/repo", names, ProcessingType.LIBRARY);

        assertSame(expected, actual);
        verify(libraryProcessor).pomsFromDirectory("/repo", names);
        verifyNoInteractions(pluginProcessor, javadocProcessor);
    }

    /**
     * Ensures that ProcessingType.PLUGINS is delegated to the @GradlePlugin PomProcessor.
     */
    @Test
    @DisplayName("Delegates PLUGINS to @GradlePlugin PomProcessor")
    void delegatesPlugins() {
        Optional<List<String>> names = Optional.empty();
        HashMap<String, Path> expected = new HashMap<>();

        when(pluginProcessor.pomsFromDirectory("/repo", names)).thenReturn(expected);

        HashMap<String, Path> actual = collector.collect("/repo", names, ProcessingType.PLUGINS);

        assertSame(expected, actual);
        verify(pluginProcessor).pomsFromDirectory("/repo", names);
        verifyNoInteractions(libraryProcessor, javadocProcessor);
    }

    /**
     * Ensures that ProcessingType.JAVADOC is delegated to the @Javadoc PomProcessor.
     */
    @Test
    @DisplayName("Delegates JAVADOC to @Javadoc PomProcessor")
    void delegatesJavadoc() {
        Optional<List<String>> names = Optional.empty();
        HashMap<String, Path> expected = new HashMap<>();

        when(javadocProcessor.pomsFromDirectory("/repo", names)).thenReturn(expected);

        HashMap<String, Path> actual = collector.collect("/repo", names, ProcessingType.JAVADOC);

        assertSame(expected, actual);
        verify(javadocProcessor).pomsFromDirectory("/repo", names);
        verifyNoInteractions(libraryProcessor, pluginProcessor);
    }

    /**
     * Ensures null processing type is rejected explicitly.
     */
    @Test
    @DisplayName("Rejects null processing type")
    void rejectsNullProcessingType() {
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> collector.collect("/repo", Optional.empty(), null)
        );
        assertEquals("Processing type can not be null!", ex.getMessage());
        verifyNoInteractions(libraryProcessor, pluginProcessor, javadocProcessor);
    }

    /**
     * Ensures the collector passes through the exact Optional instance (contract: do not unwrap/rebuild).
     */
    @Test
    @DisplayName("Passes through Optional artifact names as-is")
    void passesThroughOptionalAsIs() {
        Optional<List<String>> names = Optional.of(List.of("foo"));
        HashMap<String, Path> expected = new HashMap<>();

        when(pluginProcessor.pomsFromDirectory("/repo", names)).thenReturn(expected);

        HashMap<String, Path> actual = collector.collect("/repo", names, ProcessingType.PLUGINS);

        assertSame(expected, actual);
        verify(pluginProcessor).pomsFromDirectory("/repo", names);
        verifyNoInteractions(libraryProcessor, javadocProcessor);
    }
}
