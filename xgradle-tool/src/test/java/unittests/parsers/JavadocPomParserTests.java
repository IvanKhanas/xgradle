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
package unittests.parsers;

import unittests.PomXmlBuilder;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.altlinux.xgradle.interfaces.containers.PomContainer;
import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Javadoc;
import org.altlinux.xgradle.impl.caches.CachesModule;
import org.altlinux.xgradle.impl.model.ModelModule;
import org.altlinux.xgradle.impl.parsers.ParsersModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JavadocPomParserTests {

    private PomContainer pomContainer;
    private PomParser<HashMap<String, Path>> parser;

    @BeforeEach
    void setUp() {
        pomContainer = mock(PomContainer.class);
        Logger logger = LoggerFactory.getLogger("test");

        Injector injector = Guice.createInjector(
                new ParsersModule(),
                new ModelModule(),
                new CachesModule(),
                binder -> {
                    binder.bind(PomContainer.class).toInstance(pomContainer);
                    binder.bind(Logger.class).toInstance(logger);
                }
        );

        parser = injector.getInstance(
                Key.get(new TypeLiteral<PomParser<HashMap<String, Path>>>() {}, Javadoc.class)
        );
    }

    @Test
    void includesEntryWhenJavadocJarExistsNextToPom(@TempDir Path tmp) throws Exception {
        Path pom = PomXmlBuilder.pom()
                .groupId("org.example")
                .artifactId("lib")
                .version("1.0")
                .writeTo(tmp.resolve("lib-1.0.pom"));

        Path javadoc = tmp.resolve("lib-1.0-javadoc.jar");
        Files.write(javadoc, new byte[]{0});

        when(pomContainer.getAllPoms(tmp.toString())).thenReturn(Collections.singleton(pom));

        HashMap<String, Path> result = parser.getArtifactCoords(tmp.toString(), Optional.empty());

        assertEquals(1, result.size());
        assertEquals(javadoc, result.get(pom.toString()));
    }

    @Test
    void skipsEntryWhenJavadocJarDoesNotExist(@TempDir Path tmp) throws Exception {
        Path pom = PomXmlBuilder.pom()
                .groupId("org.example")
                .artifactId("lib")
                .version("1.0")
                .writeTo(tmp.resolve("lib-1.0.pom"));

        when(pomContainer.getAllPoms(tmp.toString())).thenReturn(Collections.singleton(pom));

        HashMap<String, Path> result = parser.getArtifactCoords(tmp.toString(), Optional.empty());

        assertTrue(result.isEmpty());
    }

    @Test
    void fallsBackToParentForVersionAndResolvesJarNameAccordingly(@TempDir Path tmp) throws Exception {
        Path pom = PomXmlBuilder.pom()
                .parent("org.example", "parent", "2.5")
                .artifactId("child")
                .writeTo(tmp.resolve("child.pom"));

        Path javadoc = tmp.resolve("child-2.5-javadoc.jar");
        Files.write(javadoc, new byte[]{0});

        when(pomContainer.getAllPoms(tmp.toString())).thenReturn(Collections.singleton(pom));

        HashMap<String, Path> result = parser.getArtifactCoords(tmp.toString(), Optional.empty());

        assertEquals(1, result.size());
        assertEquals(javadoc, result.get(pom.toString()));
    }

    @Test
    void whenArtifactNamesPresentUsesGetSelectedPoms(@TempDir Path tmp) throws Exception {
        Path pom = PomXmlBuilder.pom()
                .groupId("org.example")
                .artifactId("lib")
                .version("1.0")
                .writeTo(tmp.resolve("lib-1.0.pom"));

        Path javadoc = tmp.resolve("lib-1.0-javadoc.jar");
        Files.write(javadoc, new byte[]{0});

        List<String> names = List.of("lib");
        when(pomContainer.getSelectedPoms(tmp.toString(), names)).thenReturn(Collections.singleton(pom));

        HashMap<String, Path> result = parser.getArtifactCoords(tmp.toString(), Optional.of(names));

        assertEquals(1, result.size());
        assertEquals(javadoc, result.get(pom.toString()));

        verify(pomContainer).getSelectedPoms(tmp.toString(), names);
        verify(pomContainer, never()).getAllPoms(anyString());
    }
}
