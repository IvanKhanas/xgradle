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
import org.altlinux.xgradle.interfaces.processors.PluginProcessor;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;

import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PluginManager contract")
class PluginManagerTests {

    @Mock
    private RepositoryManager repoManager;

    @Mock
    private ScopeManager scopeManager;

    @Mock
    private TransitiveDependencyManager transitiveDependencyManager;

    @Mock
    private PluginProcessor pluginProcessor;

    @Mock
    private Logger logger;

    @Mock
    private Settings settings;

    @Test
    @DisplayName("Configures repositories and processes plugins when jars directory exists")
    void configuresWhenDirExists(@TempDir Path tempDir) {
        String prev = System.getProperty("java.library.dir");
        System.setProperty("java.library.dir", tempDir.toString());
        try {
            Injector injector = Guice.createInjector(
                    Modules.override(new ManagersModule()).with(new AbstractModule() {
                        @Override
                        protected void configure() {
                            bind(RepositoryManager.class).toInstance(repoManager);
                            bind(ScopeManager.class).toInstance(scopeManager);
                            bind(TransitiveDependencyManager.class).toInstance(transitiveDependencyManager);
                            bind(PluginProcessor.class).toInstance(pluginProcessor);
                            bind(Logger.class).toInstance(logger);
                        }
                    })
            );

            PluginManager manager = injector.getInstance(PluginManager.class);
            manager.configure(settings);

            verify(repoManager).configurePluginsRepository(eq(settings), any());
            verify(pluginProcessor).process(settings);
            verify(logger, never()).warn(anyString(), any(File.class));
        } finally {
            if (prev != null) {
                System.setProperty("java.library.dir", prev);
            } else {
                System.clearProperty("java.library.dir");
            }
        }
    }

    @Test
    @DisplayName("Warns and skips when jars directory missing")
    void warnsWhenMissingDir(@TempDir Path tempDir) {
        String prev = System.getProperty("java.library.dir");
        System.setProperty("java.library.dir", tempDir.resolve("missing").toString());
        try {
            Injector injector = Guice.createInjector(
                    Modules.override(new ManagersModule()).with(new AbstractModule() {
                        @Override
                        protected void configure() {
                            bind(RepositoryManager.class).toInstance(repoManager);
                            bind(ScopeManager.class).toInstance(scopeManager);
                            bind(TransitiveDependencyManager.class).toInstance(transitiveDependencyManager);
                            bind(PluginProcessor.class).toInstance(pluginProcessor);
                            bind(Logger.class).toInstance(logger);
                        }
                    })
            );

            PluginManager manager = injector.getInstance(PluginManager.class);
            manager.configure(settings);

            verify(logger).warn(startsWith("System jars directory does not exist"), any(File.class));
            verifyNoInteractions(repoManager, pluginProcessor);
        } finally {
            if (prev != null) {
                System.setProperty("java.library.dir", prev);
            } else {
                System.clearProperty("java.library.dir");
            }
        }
    }
}
