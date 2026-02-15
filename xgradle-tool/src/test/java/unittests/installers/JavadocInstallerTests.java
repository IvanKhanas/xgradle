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
import org.altlinux.xgradle.interfaces.installers.ArtifactsInstaller;
import org.altlinux.xgradle.interfaces.installers.JavadocInstaller;
import org.altlinux.xgradle.impl.config.ToolConfig;
import org.altlinux.xgradle.impl.enums.ProcessingType;
import org.altlinux.xgradle.impl.installers.InstallersModule;

import org.junit.jupiter.api.AfterEach;
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
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static unittests.PomXmlBuilder.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JavadocInstaller")
class JavadocInstallerTests {

    @TempDir
    Path tempDir;

    @Mock
    ArtifactCollector artifactCollector;

    @Mock
    ToolConfig toolConfig;

    @Mock
    Logger logger;

    @Mock
    ArtifactsInstaller artifactsInstaller;

    private JavadocInstaller installer;
    private Path mfiles;

    @BeforeEach
    void setUp() throws Exception {
        mfiles = Paths.get(".").toAbsolutePath().resolve(".mfiles-javadoc");
        Files.deleteIfExists(mfiles);

        Injector injector = Guice.createInjector(
                Modules.override(new InstallersModule())
                        .with(new AbstractModule() {
                            @Override
                            protected void configure() {
                                bind(ArtifactsInstaller.class).toInstance(artifactsInstaller);
                                bind(ArtifactCollector.class).toInstance(artifactCollector);
                                bind(ToolConfig.class).toInstance(toolConfig);
                                bind(Logger.class).toInstance(logger);
                            }
                        })
        );

        installer = injector.getInstance(JavadocInstaller.class);
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(mfiles);
    }

    @Test
    @DisplayName("Is created by Guice")
    void installerIsCreated() {
        assertNotNull(installer);
    }

    @Test
    @DisplayName("Copies javadoc jar to target dir and writes .mfiles-javadoc entry")
    void copiesJavadocAndUpdatesMfiles() throws Exception {
        Path repo = tempDir.resolve("repo");
        Files.createDirectories(repo);

        Path jarDir = tempDir.resolve("install");
        Files.createDirectories(jarDir);

        Path pomPath = pom()
                .groupId("g")
                .artifactId("lib")
                .version("1")
                .writeTo(repo.resolve("lib.pom"));

        Path javadocJar = repo.resolve("lib-1-javadoc.jar");
        writeBytes(javadocJar, "JD".getBytes(StandardCharsets.UTF_8));

        HashMap<String, Path> map = new HashMap<>();
        map.put(pomPath.toString(), javadocJar);

        when(artifactCollector.collect(eq(repo.toString()), eq(Optional.empty()), eq(ProcessingType.JAVADOC)))
                .thenReturn(map);

        when(toolConfig.getInstallPrefix()).thenReturn(null);

        installer.installJavadoc(repo.toString(), Optional.empty(), jarDir.toString());

        Path copied = jarDir.resolve("lib-javadoc.jar");
        assertTrue(Files.exists(copied));
        assertArrayEquals("JD".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(copied));

        assertTrue(Files.exists(mfiles));
        String content = Files.readString(mfiles, StandardCharsets.UTF_8).trim();
        assertEquals(jarDir.toString(), content);
    }

    @Test
    @DisplayName("Writes stripped path to .mfiles-javadoc when install prefix is set")
    void writesStrippedPathWhenInstallPrefixSet() throws Exception {
        Path repo = tempDir.resolve("repo");
        Files.createDirectories(repo);

        Path prefix = tempDir.resolve("prefix");
        Path jarDir = prefix.resolve("usr/share/java");
        Files.createDirectories(jarDir);

        Path pomPath = pom()
                .groupId("g")
                .artifactId("lib")
                .version("1")
                .writeTo(repo.resolve("lib.pom"));

        Path javadocJar = repo.resolve("lib-1-javadoc.jar");
        writeBytes(javadocJar, "JD".getBytes(StandardCharsets.UTF_8));

        HashMap<String, Path> map = new HashMap<>();
        map.put(pomPath.toString(), javadocJar);

        when(artifactCollector.collect(eq(repo.toString()), eq(Optional.empty()), eq(ProcessingType.JAVADOC)))
                .thenReturn(map);

        when(toolConfig.getInstallPrefix()).thenReturn(prefix.toString());

        installer.installJavadoc(repo.toString(), Optional.empty(), jarDir.toString());

        assertTrue(Files.exists(mfiles));
        String content = Files.readString(mfiles, StandardCharsets.UTF_8).trim();

        String expected = jarDir.toString().substring(prefix.toString().length());
        assertEquals(expected, content);
    }

    @Test
    @DisplayName("Does not duplicate .mfiles-javadoc entry when called twice")
    void doesNotDuplicateMfilesEntry() throws Exception {
        Path repo = tempDir.resolve("repo");
        Files.createDirectories(repo);

        Path jarDir = tempDir.resolve("install");
        Files.createDirectories(jarDir);

        Path pomPath = pom()
                .groupId("g")
                .artifactId("lib")
                .version("1")
                .writeTo(repo.resolve("lib.pom"));

        Path javadocJar = repo.resolve("lib-1-javadoc.jar");
        writeBytes(javadocJar, "JD".getBytes(StandardCharsets.UTF_8));

        HashMap<String, Path> map = new HashMap<>();
        map.put(pomPath.toString(), javadocJar);

        when(artifactCollector.collect(eq(repo.toString()), eq(Optional.empty()), eq(ProcessingType.JAVADOC)))
                .thenReturn(map);

        when(toolConfig.getInstallPrefix()).thenReturn(null);

        installer.installJavadoc(repo.toString(), Optional.empty(), jarDir.toString());

        assertTrue(Files.exists(mfiles));
        String first = Files.readString(mfiles, StandardCharsets.UTF_8);

        installer.installJavadoc(repo.toString(), Optional.empty(), jarDir.toString());

        assertTrue(Files.exists(mfiles));
        String second = Files.readString(mfiles, StandardCharsets.UTF_8);

        assertEquals(first, second);
    }

    @Test
    @DisplayName("Does nothing when collector returns empty map")
    void doesNothingWhenNoArtifacts() throws Exception {
        Path repo = tempDir.resolve("repo");
        Files.createDirectories(repo);

        Path jarDir = tempDir.resolve("install");
        Files.createDirectories(jarDir);

        when(artifactCollector.collect(eq(repo.toString()), eq(Optional.empty()), eq(ProcessingType.JAVADOC)))
                .thenReturn(new HashMap<>());

        installer.installJavadoc(repo.toString(), Optional.empty(), jarDir.toString());

        assertFalse(Files.exists(mfiles));
        try (var s = Files.list(jarDir)) {
            assertTrue(s.findAny().isEmpty());
        }
    }
}
    