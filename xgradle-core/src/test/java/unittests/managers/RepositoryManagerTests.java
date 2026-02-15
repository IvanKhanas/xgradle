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
import org.altlinux.xgradle.impl.managers.ManagersModule;
import org.altlinux.xgradle.interfaces.managers.PluginManager;
import org.altlinux.xgradle.interfaces.managers.RepositoryManager;
import org.altlinux.xgradle.interfaces.managers.ScopeManager;
import org.altlinux.xgradle.interfaces.managers.TransitiveDependencyManager;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.FlatDirectoryArtifactRepository;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.plugin.management.PluginManagementSpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.mockito.junit.jupiter.MockitoExtension;
import org.gradle.testfixtures.ProjectBuilder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RepositoryManager contract")
class RepositoryManagerTests {

    @Mock
    private Logger logger;

    @Mock
    private Settings settings;

    @Mock
    private PluginManager pluginManager;

    @Mock
    private ScopeManager scopeManager;

    @Mock
    private TransitiveDependencyManager transitiveDependencyManager;

    @Mock
    private PluginManagementSpec pluginManagement;

    @Mock
    private RepositoryHandler repos;

    @Mock
    private FlatDirectoryArtifactRepository flatRepo;

    @Test
    @DisplayName("configureDependenciesRepository adds flat repo to front")
    void configuresDependenciesRepo(@TempDir Path tempDir) throws Exception {
        Path base = tempDir.resolve("repo");
        Files.createDirectories(base);
        Files.createDirectories(base.resolve("nested"));

        Injector injector = Guice.createInjector(
                Modules.override(new ManagersModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(PluginManager.class).toInstance(pluginManager);
                        bind(ScopeManager.class).toInstance(scopeManager);
                        bind(TransitiveDependencyManager.class).toInstance(transitiveDependencyManager);
                        bind(Logger.class).toInstance(logger);
                    }
                })
        );

        RepositoryManager manager = injector.getInstance(RepositoryManager.class);
        Project project = ProjectBuilder.builder().build();
        RepositoryHandler repos = project.getRepositories();

        manager.configureDependenciesRepository(repos, base.toFile());

        assertEquals(1, repos.size());
        assertTrue(repos.get(0) instanceof FlatDirectoryArtifactRepository);

        FlatDirectoryArtifactRepository flat = (FlatDirectoryArtifactRepository) repos.get(0);
        Set<File> dirs = flat.getDirs();
        assertTrue(dirs.contains(base.toFile()));
    }

    @Test
    @DisplayName("configureDependenciesRepository rejects non-directory")
    void rejectsInvalidDirectory(@TempDir Path tempDir) throws Exception {
        Path file = Files.writeString(tempDir.resolve("file.txt"), "x");

        Injector injector = Guice.createInjector(
                Modules.override(new ManagersModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(PluginManager.class).toInstance(pluginManager);
                        bind(ScopeManager.class).toInstance(scopeManager);
                        bind(TransitiveDependencyManager.class).toInstance(transitiveDependencyManager);
                        bind(Logger.class).toInstance(logger);
                    }
                })
        );

        RepositoryManager manager = injector.getInstance(RepositoryManager.class);
        Project project = ProjectBuilder.builder().build();
        RepositoryHandler repos = project.getRepositories();

        assertThrows(GradleException.class, () -> manager.configureDependenciesRepository(repos, file.toFile()));
    }

    @Test
    @DisplayName("configurePluginsRepository wires flatDir with scanned dirs")
    void configuresPluginRepositories(@TempDir Path tempDir) throws Exception {
        Path base = tempDir.resolve("plugins");
        Files.createDirectories(base);
        Files.createDirectories(base.resolve("nested"));

        when(settings.getPluginManagement()).thenReturn(pluginManagement);
        when(pluginManagement.getRepositories()).thenReturn(repos);

        when(repos.flatDir(any(Action.class))).thenAnswer((Answer<FlatDirectoryArtifactRepository>) invocation -> {
            @SuppressWarnings("unchecked")
            Action<FlatDirectoryArtifactRepository> action = invocation.getArgument(0);
            action.execute(flatRepo);
            return flatRepo;
        });

        Injector injector = Guice.createInjector(
                Modules.override(new ManagersModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(PluginManager.class).toInstance(pluginManager);
                        bind(ScopeManager.class).toInstance(scopeManager);
                        bind(TransitiveDependencyManager.class).toInstance(transitiveDependencyManager);
                        bind(Logger.class).toInstance(logger);
                    }
                })
        );

        RepositoryManager manager = injector.getInstance(RepositoryManager.class);
        manager.configurePluginsRepository(settings, base.toFile());

        verify(repos).flatDir(any(Action.class));
        verify(flatRepo).setName("SystemPluginsRepo");
        verify(flatRepo, atLeastOnce()).dir(any(File.class));
    }
}
