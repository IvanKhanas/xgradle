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

import com.google.inject.*;

import org.altlinux.xgradle.interfaces.containers.PomContainer;
import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.GradlePlugin;
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

public class GradlePluginPomParserTests {

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
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(PomContainer.class).toInstance(pomContainer);
                        bind(Logger.class).toInstance(logger);
                    }
                }
        );

        parser = injector.getInstance(
                Key.get(new TypeLiteral<PomParser<HashMap<String, Path>>>() {}, GradlePlugin.class)
        );
    }

    @Test
    void whenArtifactNamesEmptyReturnsEmptyAndDoesNotCallPomContainer(@TempDir Path tmp) {
        HashMap<String, Path> result = parser.getArtifactCoords(tmp.toString(), Optional.empty());
        assertTrue(result.isEmpty());
        verifyNoInteractions(pomContainer);
    }

    @Test
    void whenArtifactNamesPresentUsesGetAllPomsAndFiltersByFilenamePrefix(@TempDir Path tmp) throws Exception {
        Path pluginPom = PomXmlBuilder.pom()
                .groupId("org.example")
                .artifactId("plug")
                .version("1.0")
                .packaging("jar")
                .property("java-gradle-plugin", "true")
                .writeTo(tmp.resolve("plug-1.0.pom"));

        Path otherPom = PomXmlBuilder.pom()
                .groupId("org.example")
                .artifactId("other")
                .version("1.0")
                .packaging("jar")
                .property("java-gradle-plugin", "true")
                .writeTo(tmp.resolve("other-1.0.pom"));

        Path pluginJar = tmp.resolve("plug-1.0.jar");
        Files.write(pluginJar, new byte[]{0});

        Set<Path> all = new HashSet<Path>(Arrays.asList(pluginPom, otherPom));
        when(pomContainer.getAllPoms(tmp.toString())).thenReturn(all);

        HashMap<String, Path> result = parser.getArtifactCoords(tmp.toString(), Optional.of(Arrays.asList("plug")));

        assertEquals(1, result.size());
        assertEquals(pluginJar, result.get(pluginPom.toString()));

        verify(pomContainer, times(1)).getAllPoms(tmp.toString());
        verifyNoMoreInteractions(pomContainer);
    }

    @Test
    void pomPackagingMarkerResolvesDependencyJarAndMapsMarkerPomToDependencyJar(@TempDir Path tmp) throws Exception {
        Path markerPom = PomXmlBuilder.pom()
                .groupId("org.example")
                .artifactId("marker")
                .version("1.0")
                .packaging("pom")
                .dep("org.dep", "dep", "2.0", null)
                .writeTo(tmp.resolve("marker-1.0.pom"));

        Path depJar = tmp.resolve("dep-2.0.jar");
        Files.write(depJar, new byte[]{0});

        when(pomContainer.getAllPoms(tmp.toString())).thenReturn(Collections.singleton(markerPom));

        HashMap<String, Path> result = parser.getArtifactCoords(tmp.toString(), Optional.of(Arrays.asList("marker")));

        assertEquals(1, result.size());
        assertEquals(depJar, result.get(markerPom.toString()));
    }

    @Test
    void nonPomPackagingFallsBackToParentGroupIdAndUsesChildVersionForJarName(@TempDir Path tmp) throws Exception {
        Path pom = PomXmlBuilder.pom()
                .parent("org.parent", "parent", "9")
                .artifactId("pp")
                .version("1.0")
                .packaging("jar")
                .property("java-gradle-plugin", "true")
                .writeTo(tmp.resolve("pp-1.0.pom"));

        Path jar = tmp.resolve("pp-1.0.jar");
        Files.write(jar, new byte[]{0});

        when(pomContainer.getAllPoms(tmp.toString())).thenReturn(Collections.singleton(pom));

        HashMap<String, Path> result = parser.getArtifactCoords(tmp.toString(), Optional.of(Arrays.asList("pp")));

        assertEquals(1, result.size());
        assertEquals(jar, result.get(pom.toString()));
    }
}
