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
package unittests.services;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

import org.altlinux.xgradle.interfaces.containers.PomContainer;
import org.altlinux.xgradle.interfaces.redactors.PomRedactor;
import org.altlinux.xgradle.interfaces.services.PomService;
import org.altlinux.xgradle.impl.services.ServicesModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PomRedactorService")
class PomRedactorServiceTests {

    @TempDir
    Path tempDir;

    private PomService service;
    private PomRedactor redactor;
    private PomContainer pomContainer;

    @BeforeEach
    void setUp() {
        redactor = mock(PomRedactor.class);
        pomContainer = mock(PomContainer.class);

        Injector injector = Guice.createInjector(
                Modules.override(new ServicesModule())
                        .with(new AbstractModule() {
                            @Override
                            protected void configure() {
                                bind(PomRedactor.class).toInstance(redactor);
                                bind(PomContainer.class).toInstance(pomContainer);
                            }
                        })
        );

        service = injector.getInstance(PomService.class);
    }

    @Test
    @DisplayName("Is created by Guice")
    void isCreatedByGuice() {
        assertNotNull(service);
    }

    @Test
    @DisplayName("addDependency: non-recursive delegates to redactor once")
    void addDependencyNonRecursiveDelegatesOnce() {
        Path pom = tempDir.resolve("a.pom");

        service.addDependency(pom, "g:a:1:test", false);

        verify(redactor, times(1)).addDependency(pom, "g:a:1:test");
        verifyNoInteractions(pomContainer);
    }

    @Test
    @DisplayName("addDependency: recursive delegates to redactor for each pom returned by container")
    void addDependencyRecursiveDelegatesForEveryPom() {
        Path dir = tempDir.resolve("repo");
        Path p1 = tempDir.resolve("1.pom");
        Path p2 = tempDir.resolve("2.pom");

        when(pomContainer.getAllPoms(dir.toString())).thenReturn(Set.of(p1, p2));

        service.addDependency(dir, "g:a:1:test", true);

        verify(pomContainer, times(1)).getAllPoms(dir.toString());
        verify(redactor, times(1)).addDependency(p1, "g:a:1:test");
        verify(redactor, times(1)).addDependency(p2, "g:a:1:test");
    }

    @Test
    @DisplayName("removeDependency: non-recursive delegates to redactor once")
    void removeDependencyNonRecursiveDelegatesOnce() {
        Path pom = tempDir.resolve("a.pom");

        service.removeDependency(pom, "g:a", false);

        verify(redactor, times(1)).removeDependency(pom, "g:a");
        verifyNoInteractions(pomContainer);
    }

    @Test
    @DisplayName("removeDependency: recursive delegates to redactor for each pom returned by container")
    void removeDependencyRecursiveDelegatesForEveryPom() {
        Path dir = tempDir.resolve("repo");
        Path p1 = tempDir.resolve("1.pom");
        Path p2 = tempDir.resolve("2.pom");

        when(pomContainer.getAllPoms(dir.toString())).thenReturn(Set.of(p1, p2));

        service.removeDependency(dir, "g:a", true);

        verify(pomContainer, times(1)).getAllPoms(dir.toString());
        verify(redactor, times(1)).removeDependency(p1, "g:a");
        verify(redactor, times(1)).removeDependency(p2, "g:a");
    }

    @Test
    @DisplayName("changeDependency: non-recursive delegates to redactor once")
    void changeDependencyNonRecursiveDelegatesOnce() {
        Path pom = tempDir.resolve("a.pom");

        service.changeDependency(pom, "g:a:1:test", "g:a:2:test", false);

        verify(redactor, times(1)).changeDependency(pom, "g:a:1:test", "g:a:2:test");
        verifyNoInteractions(pomContainer);
    }

    @Test
    @DisplayName("changeDependency: recursive delegates to redactor for each pom returned by container")
    void changeDependencyRecursiveDelegatesForEveryPom() {
        Path dir = tempDir.resolve("repo");
        Path p1 = tempDir.resolve("1.pom");
        Path p2 = tempDir.resolve("2.pom");

        when(pomContainer.getAllPoms(dir.toString())).thenReturn(Set.of(p1, p2));

        service.changeDependency(dir, "g:a:1:test", "g:a:2:test", true);

        verify(pomContainer, times(1)).getAllPoms(dir.toString());
        verify(redactor, times(1)).changeDependency(p1, "g:a:1:test", "g:a:2:test");
        verify(redactor, times(1)).changeDependency(p2, "g:a:1:test", "g:a:2:test");
    }

    @Test
    @DisplayName("excludeArtifacts(HashMap): returns same map when excluded list is null")
    void excludeArtifactsHashMapReturnsSameWhenNullExcludedList() {
        HashMap<String, Path> map = new HashMap<>();
        map.put(tempDir.resolve("aaa-1.pom").toString(), tempDir.resolve("aaa-1.jar"));

        HashMap<String, Path> result = service.excludeArtifacts(null, map);

        assertSame(map, result);
    }

    @Test
    @DisplayName("excludeArtifacts(HashMap): filters entries by pom filename prefix")
    void excludeArtifactsHashMapFiltersByPrefix() {
        HashMap<String, Path> map = new HashMap<>();
        map.put(tempDir.resolve("aaa-1.pom").toString(), tempDir.resolve("aaa-1.jar"));
        map.put(tempDir.resolve("bbb-1.pom").toString(), tempDir.resolve("bbb-1.jar"));

        HashMap<String, Path> filtered = service.excludeArtifacts(List.of("aaa"), map);

        assertEquals(1, filtered.size());
        assertTrue(filtered.keySet().stream().allMatch(k -> Path.of(k).getFileName().toString().startsWith("bbb")));
    }

    @Test
    @DisplayName("excludeArtifacts(Set): returns same set when excluded list is null")
    void excludeArtifactsSetReturnsSameWhenNullExcludedList() {
        Set<Path> set = Set.of(tempDir.resolve("aaa-1.pom"));

        Set<Path> result = service.excludeArtifacts(null, set);

        assertSame(set, result);
    }

    @Test
    @DisplayName("excludeArtifacts(Set): filters entries by pom filename prefix")
    void excludeArtifactsSetFiltersByPrefix() {
        Set<Path> set = Set.of(
                tempDir.resolve("aaa-1.pom"),
                tempDir.resolve("bbb-1.pom")
        );

        Set<Path> filtered = service.excludeArtifacts(List.of("aaa"), set);

        assertEquals(1, filtered.size());
        assertTrue(filtered.iterator().next().getFileName().toString().startsWith("bbb"));
    }

    @Test
    @DisplayName("removeParentBlocks(HashMap): does nothing when removeParentPoms is null")
    void removeParentBlocksHashMapDoesNothingWhenNullPatterns() {
        HashMap<String, Path> artifacts = new HashMap<>();
        artifacts.put(tempDir.resolve("aaa-1.pom").toString(), tempDir.resolve("aaa-1.jar"));

        service.removeParentBlocks(artifacts, null);

        verifyNoInteractions(redactor);
    }

    @Test
    @DisplayName("removeParentBlocks(HashMap): removes parent for all when pattern is 'all'")
    void removeParentBlocksHashMapRemovesForAll() {
        HashMap<String, Path> artifacts = new HashMap<>();
        Path p1 = tempDir.resolve("aaa-1.pom");
        Path p2 = tempDir.resolve("bbb-1.pom");
        artifacts.put(p1.toString(), tempDir.resolve("aaa-1.jar"));
        artifacts.put(p2.toString(), tempDir.resolve("bbb-1.jar"));

        service.removeParentBlocks(artifacts, List.of("all"));

        verify(redactor, times(1)).removeParent(p1);
        verify(redactor, times(1)).removeParent(p2);
    }

    @Test
    @DisplayName("removeParentBlocks(Set): removes parent by prefix")
    void removeParentBlocksSetRemovesByPrefix() {
        Path p1 = tempDir.resolve("aaa-1.pom");
        Path p2 = tempDir.resolve("bbb-1.pom");

        service.removeParentBlocks(Set.of(p1, p2), List.of("aaa"));

        verify(redactor, times(1)).removeParent(p1);
        verify(redactor, never()).removeParent(p2);
    }
}
