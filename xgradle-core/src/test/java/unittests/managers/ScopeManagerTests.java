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
import org.altlinux.xgradle.interfaces.managers.PluginManager;
import org.altlinux.xgradle.interfaces.managers.RepositoryManager;
import org.altlinux.xgradle.interfaces.managers.ScopeManager;
import org.altlinux.xgradle.interfaces.managers.TransitiveDependencyManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScopeManager contract")
class ScopeManagerTests {

    @Mock
    private PluginManager pluginManager;

    @Mock
    private RepositoryManager repositoryManager;

    @Mock
    private TransitiveDependencyManager transitiveDependencyManager;

    @Test
    @DisplayName("Updates scope by priority and returns defaults")
    void updatesScopeByPriority() {
        Injector injector = Guice.createInjector(
                Modules.override(new ManagersModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(PluginManager.class).toInstance(pluginManager);
                        bind(RepositoryManager.class).toInstance(repositoryManager);
                        bind(TransitiveDependencyManager.class).toInstance(transitiveDependencyManager);
                    }
                })
        );
        ScopeManager manager = injector.getInstance(ScopeManager.class);

        Map<String, MavenScope> scopes = new HashMap<>();
        manager.updateScope(scopes, "g:a", MavenScope.RUNTIME);
        manager.updateScope(scopes, "g:a", MavenScope.COMPILE);
        manager.updateScope(scopes, "g:a", MavenScope.TEST);

        assertEquals(MavenScope.COMPILE, scopes.get("g:a"));
        assertEquals(MavenScope.COMPILE, manager.getScope(scopes, "missing"));
        assertEquals(MavenScope.COMPILE, manager.getScope(null, "anything"));
    }
}
