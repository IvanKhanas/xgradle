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
package unittests.managers;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.altlinux.xgradle.impl.enums.MavenScope;
import org.altlinux.xgradle.impl.managers.ManagersModule;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.interfaces.managers.PluginManager;
import org.altlinux.xgradle.interfaces.managers.RepositoryManager;
import org.altlinux.xgradle.interfaces.managers.ScopeManager;
import org.altlinux.xgradle.interfaces.managers.TransitiveDependencyManager;
import org.altlinux.xgradle.interfaces.maven.PomFinder;
import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.gradle.api.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransitiveDependencyManager contract")
class TransitiveDependencyManagerTests {

    @Mock
    private PomFinder pomFinder;

    @Mock
    private PomParser pomParser;

    @Mock
    private PluginManager pluginManager;

    @Mock
    private RepositoryManager repositoryManager;

    @Mock
    private ScopeManager scopeManager;

    @Mock
    private Logger logger;

    @Test
    @DisplayName("Resolves transitives, propagates config names, and tracks skipped deps")
    void resolvesTransitivesAndSkipsMissing() {
        Injector injector = Guice.createInjector(
                Modules.override(new ManagersModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(PomFinder.class).toInstance(pomFinder);
                        bind(PomParser.class).toInstance(pomParser);
                        bind(PluginManager.class).toInstance(pluginManager);
                        bind(RepositoryManager.class).toInstance(repositoryManager);
                        bind(ScopeManager.class).toInstance(scopeManager);
                        bind(Logger.class).toInstance(logger);
                    }
                })
        );

        TransitiveDependencyManager manager = injector.getInstance(TransitiveDependencyManager.class);

        MavenCoordinate root = MavenCoordinate.builder()
                .groupId("g")
                .artifactId("a")
                .version("1")
                .pomPath(Path.of("root.pom"))
                .build();

        MavenCoordinate dep = MavenCoordinate.builder()
                .groupId("d")
                .artifactId("dep")
                .version("1")
                .scope(MavenScope.COMPILE)
                .pomPath(Path.of("dep.pom"))
                .build();

        MavenCoordinate testDep = MavenCoordinate.builder()
                .groupId("t")
                .artifactId("test")
                .version("1")
                .scope(MavenScope.TEST)
                .pomPath(Path.of("test.pom"))
                .build();

        MavenCoordinate missingDep = MavenCoordinate.builder()
                .groupId("m")
                .artifactId("missing")
                .version("1")
                .scope(MavenScope.COMPILE)
                .pomPath(Path.of("missing.pom"))
                .build();

        when(pomParser.parseDependencies(root.getPomPath()))
                .thenReturn(List.of(dep, testDep, missingDep));
        when(pomFinder.findPomForArtifact("d", "dep")).thenReturn(dep);
        when(pomFinder.findPomForArtifact("m", "missing")).thenReturn(null);

        Map<String, MavenCoordinate> systemArtifacts = new HashMap<>();
        systemArtifacts.put("g:a", root);

        Map<String, MavenScope> scopes = new HashMap<>();
        Map<String, Set<String>> configNames = new HashMap<>();
        configNames.put("g:a", Set.of("implementation"));

        Set<String> skipped = manager.configure(systemArtifacts, scopes, configNames);

        assertTrue(skipped.contains("m:missing"));
        assertTrue(systemArtifacts.containsKey("d:dep"));
        assertTrue(configNames.get("d:dep").contains("implementation"));
        assertNull(configNames.get("t:test"));

        verify(scopeManager, atLeastOnce()).updateScope(eq(scopes), eq("d:dep"), eq(MavenScope.COMPILE));
        verify(scopeManager, atLeastOnce()).updateScope(eq(scopes), eq("t:test"), eq(MavenScope.TEST));
        verify(scopeManager, atLeastOnce()).updateScope(eq(scopes), eq("m:missing"), eq(MavenScope.COMPILE));
    }

    @Test
    @DisplayName("Skips transitives that originate only from custom configurations")
    void skipsTransitivelyFromCustomConfigurations() {
        Injector injector = Guice.createInjector(
                Modules.override(new ManagersModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(PomFinder.class).toInstance(pomFinder);
                        bind(PomParser.class).toInstance(pomParser);
                        bind(PluginManager.class).toInstance(pluginManager);
                        bind(RepositoryManager.class).toInstance(repositoryManager);
                        bind(ScopeManager.class).toInstance(scopeManager);
                        bind(Logger.class).toInstance(logger);
                    }
                })
        );

        TransitiveDependencyManager manager = injector.getInstance(TransitiveDependencyManager.class);

        MavenCoordinate root = MavenCoordinate.builder()
                .groupId("g")
                .artifactId("a")
                .version("1")
                .pomPath(Path.of("root.pom"))
                .build();

        MavenCoordinate dep = MavenCoordinate.builder()
                .groupId("d")
                .artifactId("dep")
                .version("1")
                .scope(MavenScope.COMPILE)
                .pomPath(Path.of("dep.pom"))
                .build();

        when(pomParser.parseDependencies(root.getPomPath()))
                .thenReturn(List.of(dep));

        Map<String, MavenCoordinate> systemArtifacts = new HashMap<>();
        systemArtifacts.put("g:a", root);

        Map<String, MavenScope> scopes = new HashMap<>();
        Map<String, Set<String>> configNames = new HashMap<>();
        configNames.put("g:a", Set.of("customConfig"));

        Set<String> skipped = manager.configure(systemArtifacts, scopes, configNames);

        assertTrue(skipped.contains("d:dep"));
        assertFalse(systemArtifacts.containsKey("d:dep"));
        assertNull(configNames.get("d:dep"));
        verify(pomFinder, never()).findPomForArtifact("d", "dep");
    }
}
