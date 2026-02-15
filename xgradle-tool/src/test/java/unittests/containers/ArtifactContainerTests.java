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
package unittests.containers;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

import org.altlinux.xgradle.interfaces.collectors.ArtifactCollector;
import org.altlinux.xgradle.interfaces.collectors.PomCollector;
import org.altlinux.xgradle.interfaces.containers.ArtifactContainer;
import org.altlinux.xgradle.impl.containers.ContainersModule;
import org.altlinux.xgradle.impl.enums.ProcessingType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArtifactContainer contract")
class ArtifactContainerTests {

    @Mock
    private ArtifactCollector artifactCollector;

    @Mock
    private PomCollector pomCollector;

    private ArtifactContainer container;

    @BeforeEach
    void setUp() {
        Injector injector = Guice.createInjector(
                Modules.override(new ContainersModule())
                        .with(new AbstractModule() {
                            @Override
                            protected void configure() {
                                bind(ArtifactCollector.class).toInstance(artifactCollector);
                                bind(PomCollector.class).toInstance(pomCollector);
                            }
                        })
        );

        container = injector.getInstance(ArtifactContainer.class);
    }

    /**
     * Verifies that Guice can create the ArtifactContainer implementation.
     */
    @Test
    @DisplayName("Guice creates ArtifactContainer implementation")
    void artifactContainerIsCreated() {
        assertNotNull(container);
    }

    /**
     * Ensures getArtifacts delegates to ArtifactCollector.collect when artifactNames is present.
     */
    @Test
    @DisplayName("getArtifacts delegates to ArtifactCollector.collect when artifactNames is present")
    void getArtifactsDelegatesWhenArtifactNamesPresent() {
        Optional<List<String>> names = Optional.of(List.of("a", "b"));
        HashMap<String, Path> expected = new HashMap<>();
        expected.put("g:a:1", Path.of("/repo/a.pom"));

        when(artifactCollector.collect("/repo", names, ProcessingType.LIBRARY)).thenReturn(expected);

        HashMap<String, Path> actual = container.getArtifacts("/repo", names, ProcessingType.LIBRARY);

        assertSame(expected, actual);
        verify(artifactCollector).collect("/repo", names, ProcessingType.LIBRARY);
        verifyNoMoreInteractions(artifactCollector);
    }

    /**
     * Ensures getArtifacts delegates to ArtifactCollector.collect with Optional.empty when artifactNames is empty.
     */
    @Test
    @DisplayName("getArtifacts delegates to ArtifactCollector.collect with Optional.empty when artifactNames is empty")
    void getArtifactsDelegatesWhenArtifactNamesEmpty() {
        HashMap<String, Path> expected = new HashMap<>();
        expected.put("g:b:2", Path.of("/repo/b.pom"));

        when(artifactCollector.collect("/repo", Optional.empty(), ProcessingType.PLUGINS)).thenReturn(expected);

        HashMap<String, Path> actual = container.getArtifacts("/repo", Optional.empty(), ProcessingType.PLUGINS);

        assertSame(expected, actual);
        verify(artifactCollector).collect("/repo", Optional.empty(), ProcessingType.PLUGINS);
        verifyNoMoreInteractions(artifactCollector);
    }

    /**
     * Ensures getArtifactPaths returns the values view of the map returned by ArtifactCollector.collect.
     */
    @Test
    @DisplayName("getArtifactPaths returns paths from collected artifacts")
    void getArtifactPathsReturnsValuesFromCollectedMap() {
        HashMap<String, Path> collected = new HashMap<>();
        collected.put("g:a:1", Path.of("/repo/a.jar"));
        collected.put("g:b:2", Path.of("/repo/b.jar"));

        when(artifactCollector.collect("/repo", Optional.empty(), ProcessingType.LIBRARY)).thenReturn(collected);

        Collection<Path> paths = container.getArtifactPaths("/repo", Optional.empty(), ProcessingType.LIBRARY);

        assertEquals(new HashSet<>(collected.values()), new HashSet<>(paths));
        verify(artifactCollector).collect("/repo", Optional.empty(), ProcessingType.LIBRARY);
        verifyNoMoreInteractions(artifactCollector);
    }

    /**
     * Ensures getArtifactSignatures returns the keySet view of the map returned by ArtifactCollector.collect.
     */
    @Test
    @DisplayName("getArtifactSignatures returns signatures from collected artifacts")
    void getArtifactSignaturesReturnsKeysFromCollectedMap() {
        HashMap<String, Path> collected = new HashMap<>();
        collected.put("g:a:1", Path.of("/repo/a.jar"));
        collected.put("g:b:2", Path.of("/repo/b.jar"));

        when(artifactCollector.collect("/repo", Optional.empty(), ProcessingType.LIBRARY)).thenReturn(collected);

        Collection<String> signatures = container.getArtifactSignatures("/repo", Optional.empty(), ProcessingType.LIBRARY);

        assertEquals(new HashSet<>(collected.keySet()), new HashSet<>(signatures));
        verify(artifactCollector).collect("/repo", Optional.empty(), ProcessingType.LIBRARY);
        verifyNoMoreInteractions(artifactCollector);
    }

    /**
     * Ensures exceptions thrown by ArtifactCollector.collect are propagated unchanged by getArtifacts.
     */
    @Test
    @DisplayName("getArtifacts propagates collector exceptions")
    void getArtifactsPropagatesCollectorExceptions() {
        RuntimeException boom = new RuntimeException("boom");

        when(artifactCollector.collect("/repo", Optional.empty(), ProcessingType.LIBRARY)).thenThrow(boom);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> container.getArtifacts("/repo", Optional.empty(), ProcessingType.LIBRARY)
        );
        assertSame(boom, ex);

        verify(artifactCollector).collect("/repo", Optional.empty(), ProcessingType.LIBRARY);
        verifyNoMoreInteractions(artifactCollector);
    }

    /**
     * Ensures exceptions thrown by ArtifactCollector.collect are propagated unchanged by getArtifactPaths.
     */
    @Test
    @DisplayName("getArtifactPaths propagates collector exceptions")
    void getArtifactPathsPropagatesCollectorExceptions() {
        RuntimeException boom = new RuntimeException("boom");

        when(artifactCollector.collect("/repo", Optional.empty(), ProcessingType.LIBRARY)).thenThrow(boom);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> container.getArtifactPaths("/repo", Optional.empty(), ProcessingType.LIBRARY)
        );
        assertSame(boom, ex);

        verify(artifactCollector).collect("/repo", Optional.empty(), ProcessingType.LIBRARY);
        verifyNoMoreInteractions(artifactCollector);
    }

    /**
     * Ensures exceptions thrown by ArtifactCollector.collect are propagated unchanged by getArtifactSignatures.
     */
    @Test
    @DisplayName("getArtifactSignatures propagates collector exceptions")
    void getArtifactSignaturesPropagatesCollectorExceptions() {
        RuntimeException boom = new RuntimeException("boom");

        when(artifactCollector.collect("/repo", Optional.empty(), ProcessingType.LIBRARY)).thenThrow(boom);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> container.getArtifactSignatures("/repo", Optional.empty(), ProcessingType.LIBRARY)
        );
        assertSame(boom, ex);

        verify(artifactCollector).collect("/repo", Optional.empty(), ProcessingType.LIBRARY);
        verifyNoMoreInteractions(artifactCollector);
    }

    /**
     * Verifies current behavior: passing null Optional for artifactNames causes NullPointerException.
     */
    @Test
    @DisplayName("Null Optional artifactNames is rejected (current behavior)")
    void nullOptionalArtifactNamesIsRejected() {
        assertThrows(
                NullPointerException.class,
                () -> container.getArtifacts("/repo", null, ProcessingType.LIBRARY)
        );
        verifyNoInteractions(artifactCollector);
    }

    /**
     * Verifies current behavior: null searchingDirectory is passed through to the collector.
     */
    @Test
    @DisplayName("Null searchingDirectory is passed through (current behavior)")
    void nullSearchingDirectoryIsPassedThrough() {
        HashMap<String, Path> expected = new HashMap<>();
        when(artifactCollector.collect(null, Optional.empty(), ProcessingType.LIBRARY)).thenReturn(expected);

        HashMap<String, Path> actual = container.getArtifacts(null, Optional.empty(), ProcessingType.LIBRARY);

        assertSame(expected, actual);
        verify(artifactCollector).collect(null, Optional.empty(), ProcessingType.LIBRARY);
        verifyNoMoreInteractions(artifactCollector);
    }

    /**
     * Verifies current behavior: null processingType is passed through to the collector (container does not validate).
     */
    @Test
    @DisplayName("Null processingType is passed through (current behavior)")
    void nullProcessingTypeIsPassedThrough() {
        HashMap<String, Path> expected = new HashMap<>();
        when(artifactCollector.collect("/repo", Optional.empty(), null)).thenReturn(expected);

        HashMap<String, Path> actual = container.getArtifacts("/repo", Optional.empty(), null);

        assertSame(expected, actual);
        verify(artifactCollector).collect("/repo", Optional.empty(), null);
        verifyNoMoreInteractions(artifactCollector);
    }
}
