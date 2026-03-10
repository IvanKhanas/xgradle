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
package unittests.installers;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

import org.altlinux.xgradle.interfaces.collectors.ArtifactCollector;
import org.altlinux.xgradle.interfaces.containers.ArtifactContainer;
import org.altlinux.xgradle.interfaces.installers.ArtifactsInstaller;
import org.altlinux.xgradle.interfaces.resolvers.PluginPomChainResolver;
import org.altlinux.xgradle.interfaces.resolvers.PluginPomChainResult;
import org.altlinux.xgradle.impl.cli.CliArgumentsContainer;
import org.altlinux.xgradle.impl.config.ToolConfig;
import org.altlinux.xgradle.impl.enums.ProcessingType;
import org.altlinux.xgradle.impl.installers.InstallersModule;

import org.apache.maven.model.Model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.HashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static unittests.PomXmlBuilder.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArtifactsInstaller")
class ArtifactsInstallerTests {

    @TempDir
    Path tempDir;

    @Mock
    ArtifactContainer artifactContainer;

    @Mock
    ArtifactCollector artifactCollector;

    @Mock
    CliArgumentsContainer cliArgs;

    @Mock
    Logger logger;

    @Mock
    PluginPomChainResolver pomChainResolver;

    private ToolConfig toolConfig;
    private ArtifactsInstaller installer;

    @BeforeEach
    void setUp() {
        toolConfig = new ToolConfig(cliArgs);

        Injector injector = Guice.createInjector(
                Modules.override(new InstallersModule())
                        .with(new AbstractModule() {
                            @Override
                            protected void configure() {
                                bind(ArtifactContainer.class).toInstance(artifactContainer);
                                bind(ArtifactCollector.class).toInstance(artifactCollector);
                                bind(CliArgumentsContainer.class).toInstance(cliArgs);
                                bind(ToolConfig.class).toInstance(toolConfig);
                                bind(Logger.class).toInstance(logger);
                                bind(PluginPomChainResolver.class).toInstance(pomChainResolver);
                            }
                        })
        );

        installer = injector.getInstance(ArtifactsInstaller.class);
    }

    @Test
    @DisplayName("Is created by Guice")
    void isCreatedByGuice() {
        assertNotNull(installer);
    }

    @Test
    @DisplayName("Copies chain POMs by artifactId and copies a JAR once based on main POM (packaging != pom)")
    void copiesChainPomsAndJarOnce() throws Exception {
        Path repo = tempDir.resolve("repo");
        Files.createDirectories(repo);

        Path outPoms = tempDir.resolve("out-poms");
        Path outJars = tempDir.resolve("out-jars");

        Path jar = repo.resolve("in.jar");
        writeBytes(jar, "JAR".getBytes(StandardCharsets.UTF_8));

        Path mainPom = pom()
                .groupId("g")
                .artifactId("plugin-main")
                .version("1.0")
                .packaging("jar")
                .writeTo(repo.resolve("main.pom"));

        Path metaPom = pom()
                .groupId("g")
                .artifactId("plugin-meta")
                .version("1.0")
                .packaging("pom")
                .writeTo(repo.resolve("meta.pom"));

        Path parentPom = pom()
                .groupId("g")
                .artifactId("plugin-parent")
                .version("1.0")
                .packaging("pom")
                .writeTo(repo.resolve("parent.pom"));

        HashMap<String, Path> artifacts = new HashMap<>();
        artifacts.put(mainPom.toString(), jar);
        artifacts.put(metaPom.toString(), jar);

        HashMap<Path, Model> pomModels = new HashMap<>();
        pomModels.put(mainPom, readModel(mainPom));
        pomModels.put(metaPom, readModel(metaPom));
        pomModels.put(parentPom, readModel(parentPom));

        when(pomChainResolver.resolve(eq(repo.toString()), eq(List.of()), eq(artifacts)))
                .thenReturn(new PluginPomChainResult(pomModels, Set.of(mainPom, metaPom, parentPom)));

        when(artifactContainer.getArtifacts(eq(repo.toString()), eq(List.of()), eq(ProcessingType.PLUGINS)))
                .thenReturn(artifacts);

        installer.install(
                repo.toString(),
                List.of(),
                outPoms.toString(),
                outJars.toString(),
                ProcessingType.PLUGINS
        );

        verify(artifactContainer, times(1)).getArtifacts(repo.toString(), List.of(), ProcessingType.PLUGINS);
        verify(pomChainResolver, times(1)).resolve(repo.toString(), List.of(), artifacts);

        assertTrue(Files.exists(outPoms.resolve("plugin-main.pom")));
        assertTrue(Files.exists(outPoms.resolve("plugin-meta.pom")));
        assertTrue(Files.exists(outPoms.resolve("plugin-parent.pom")));

        assertTrue(Files.exists(outJars.resolve("plugin-main.jar")));
        assertArrayEquals(
                "JAR".getBytes(StandardCharsets.UTF_8),
                Files.readAllBytes(outJars.resolve("plugin-main.jar"))
        );

        long jarCount;
        try (var s = Files.list(outJars)) {
            jarCount = s.count();
        }
        assertEquals(1L, jarCount);
    }

    @Test
    @DisplayName("Deduplicates jar copy when multiple poms point to the same jar, using chain resolution")
    void deduplicatesJarCopy() throws Exception {
        Path repo = tempDir.resolve("repo");
        Files.createDirectories(repo);

        Path outPoms = tempDir.resolve("out-poms");
        Path outJars = tempDir.resolve("out-jars");

        Path jar = repo.resolve("in.jar");
        writeBytes(jar, "JAR".getBytes(StandardCharsets.UTF_8));

        Path pom1 = pom()
                .groupId("g")
                .artifactId("a")
                .version("1.0")
                .packaging("pom")
                .writeTo(repo.resolve("a.pom"));

        Path pom2 = pom()
                .groupId("g")
                .artifactId("b")
                .version("1.0")
                .packaging("pom")
                .writeTo(repo.resolve("b.pom"));

        HashMap<String, Path> artifacts = new HashMap<>();
        artifacts.put(pom1.toString(), jar);
        artifacts.put(pom2.toString(), jar);

        HashMap<Path, Model> pomModels = new HashMap<>();
        pomModels.put(pom1, readModel(pom1));
        pomModels.put(pom2, readModel(pom2));

        when(pomChainResolver.resolve(eq(repo.toString()), eq(List.of()), eq(artifacts)))
                .thenReturn(new PluginPomChainResult(pomModels, Set.of(pom1, pom2)));

        when(artifactContainer.getArtifacts(eq(repo.toString()), eq(List.of()), eq(ProcessingType.PLUGINS)))
                .thenReturn(artifacts);

        installer.install(
                repo.toString(),
                List.of(),
                outPoms.toString(),
                outJars.toString(),
                ProcessingType.PLUGINS
        );

        assertTrue(Files.exists(outPoms.resolve("a.pom")));
        assertTrue(Files.exists(outPoms.resolve("b.pom")));

        long jarCount;
        try (var s = Files.list(outJars)) {
            jarCount = s.filter(p -> p.getFileName().toString().endsWith(".jar")).count();
        }
        assertEquals(1L, jarCount);
    }
}
