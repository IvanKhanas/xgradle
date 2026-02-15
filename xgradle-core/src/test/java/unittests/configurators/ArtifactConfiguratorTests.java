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
package unittests.configurators;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.altlinux.xgradle.impl.configurators.ConfiguratorsModule;
import org.altlinux.xgradle.impl.managers.ManagersModule;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.interfaces.configurators.ArtifactConfigurator;
import org.altlinux.xgradle.interfaces.managers.PluginManager;
import org.altlinux.xgradle.interfaces.managers.RepositoryManager;
import org.altlinux.xgradle.interfaces.managers.TransitiveDependencyManager;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.invocation.Gradle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unittests.TestGradleUtils;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ArtifactConfigurator contract")
class ArtifactConfiguratorTests {

    @Mock
    private Gradle gradle;

    @Mock
    private PluginManager pluginManager;

    @Mock
    private RepositoryManager repositoryManager;

    @Mock
    private TransitiveDependencyManager transitiveDependencyManager;

    @Test
    @DisplayName("Adds resolved artifacts to original configuration names")
    void addsToOriginalConfiguration() {
        Project root = TestGradleUtils.newJavaProject("root");
        root.setGroup("org.root");
        TestGradleUtils.gradleWithProjects(gradle, root);

        Injector injector = Guice.createInjector(
                new ConfiguratorsModule(),
                Modules.override(new ManagersModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(PluginManager.class).toInstance(pluginManager);
                        bind(RepositoryManager.class).toInstance(repositoryManager);
                        bind(TransitiveDependencyManager.class).toInstance(transitiveDependencyManager);
                    }
                })
        );
        ArtifactConfigurator configurator = injector.getInstance(ArtifactConfigurator.class);

        MavenCoordinate coord = MavenCoordinate.builder()
                .groupId("com.acme")
                .artifactId("lib")
                .version("1.0")
                .build();

        configurator.configure(
                gradle,
                Map.of("com.acme:lib", coord),
                Map.of("com.acme:lib", Set.of("implementation")),
                Map.of(),
                Set.of(),
                Map.of()
        );

        Dependency dep = root.getConfigurations()
                .getByName("implementation")
                .getDependencies()
                .stream()
                .filter(d -> "com.acme".equals(d.getGroup()) && "lib".equals(d.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(dep);
        assertEquals("1.0", dep.getVersion());
        assertTrue(configurator.getConfigurationArtifacts()
                .getOrDefault("implementation", Set.of())
                .contains("com.acme:lib:1.0"));
    }
}
