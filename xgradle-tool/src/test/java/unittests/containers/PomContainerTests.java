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
import org.altlinux.xgradle.interfaces.collectors.PomCollector;
import org.altlinux.xgradle.interfaces.containers.ArtifactContainer;
import org.altlinux.xgradle.interfaces.containers.PomContainer;
import org.altlinux.xgradle.impl.containers.ContainersModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PomContainer contract")
class PomContainerTests {

    @Mock
    private PomCollector pomCollector;

    @Mock
    private ArtifactContainer artifactContainer;

    private PomContainer container;

    @BeforeEach
    void setUp() {
        Injector injector = Guice.createInjector(
                Modules.override(new ContainersModule())
                        .with(new AbstractModule() {
                            @Override
                            protected void configure() {
                                bind(PomCollector.class).toInstance(pomCollector);
                                bind(ArtifactContainer.class).toInstance(artifactContainer);
                            }
                        })
        );

        container = injector.getInstance(PomContainer.class);
    }

    /**
     * Verifies that Guice can create the PomContainer implementation.
     */
    @Test
    @DisplayName("Guice creates PomContainer implementation")
    void pomContainerIsCreated() {
        assertNotNull(container);
    }

    /**
     * Verifies that getAllPoms delegates to PomCollector.collectAll and returns the same Set instance.
     */
    @Test
    @DisplayName("getAllPoms delegates to PomCollector.collectAll")
    void getAllPomsDelegatesToCollector() {
        Set<Path> expected = Set.of(Path.of("/repo/a.pom"), Path.of("/repo/b.pom"));
        when(pomCollector.collectAll("/repo")).thenReturn(expected);

        Set<Path> actual = container.getAllPoms("/repo");

        assertSame(expected, actual);
        verify(pomCollector).collectAll("/repo");
        verifyNoMoreInteractions(pomCollector);
    }

    /**
     * Verifies that getSelectedPoms delegates to PomCollector.collectSelected and returns the same Set instance.
     */
    @Test
    @DisplayName("getSelectedPoms delegates to PomCollector.collectSelected")
    void getSelectedPomsDelegatesToCollector() {
        List<String> prefixes = List.of("foo", "bar");
        Set<Path> expected = Set.of(Path.of("/repo/foo-1.0.pom"));

        when(pomCollector.collectSelected("/repo", prefixes)).thenReturn(expected);

        Set<Path> actual = container.getSelectedPoms("/repo", prefixes);

        assertSame(expected, actual);
        verify(pomCollector).collectSelected("/repo", prefixes);
        verifyNoMoreInteractions(pomCollector);
    }

    /**
     * Verifies that exceptions thrown by PomCollector.collectAll are propagated unchanged.
     */
    @Test
    @DisplayName("getAllPoms propagates collector exceptions")
    void getAllPomsPropagatesCollectorExceptions() {
        RuntimeException boom = new RuntimeException("boom");
        when(pomCollector.collectAll("/repo")).thenThrow(boom);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> container.getAllPoms("/repo"));
        assertSame(boom, ex);

        verify(pomCollector).collectAll("/repo");
        verifyNoMoreInteractions(pomCollector);
    }

    /**
     * Verifies that exceptions thrown by PomCollector.collectSelected are propagated unchanged.
     */
    @Test
    @DisplayName("getSelectedPoms propagates collector exceptions")
    void getSelectedPomsPropagatesCollectorExceptions() {
        RuntimeException boom = new RuntimeException("boom");
        List<String> prefixes = List.of("foo");

        when(pomCollector.collectSelected("/repo", prefixes)).thenThrow(boom);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> container.getSelectedPoms("/repo", prefixes));
        assertSame(boom, ex);

        verify(pomCollector).collectSelected("/repo", prefixes);
        verifyNoMoreInteractions(pomCollector);
    }

    /**
     * Verifies current behavior: PomContainer does not validate null searchingDir and passes it through to the collector.
     */
    @Test
    @DisplayName("Null searchingDir is passed through (current behavior)")
    void nullSearchingDirIsPassedThrough() {
        when(pomCollector.collectAll(null)).thenReturn(Set.of());

        Set<Path> actual = container.getAllPoms(null);

        assertEquals(Set.of(), actual);
        verify(pomCollector).collectAll(null);
        verifyNoMoreInteractions(pomCollector);
    }

    /**
     * Verifies current behavior: PomContainer does not validate null artifactName list and passes it through.
     */
    @Test
    @DisplayName("Null artifactName list is passed through (current behavior)")
    void nullArtifactNamesIsPassedThrough() {
        when(pomCollector.collectSelected("/repo", null)).thenReturn(Set.of());

        Set<Path> actual = container.getSelectedPoms("/repo", null);

        assertEquals(Set.of(), actual);
        verify(pomCollector).collectSelected("/repo", null);
        verifyNoMoreInteractions(pomCollector);
    }
}
