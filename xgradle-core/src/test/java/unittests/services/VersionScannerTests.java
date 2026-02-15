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
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.services.ServicesModule;
import org.altlinux.xgradle.interfaces.maven.PomFinder;
import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.altlinux.xgradle.interfaces.services.ArtifactVerifier;
import org.altlinux.xgradle.interfaces.services.VersionScanner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VersionScanner contract")
class VersionScannerTests {

    @Mock
    private PomFinder pomFinder;

    @Mock
    private PomParser pomParser;

    @Mock
    private ArtifactVerifier verifier;

    @Test
    @DisplayName("Scans dependencies and resolves coordinates")
    void scansDependencies() {
        MavenCoordinate coord = MavenCoordinate.builder()
                .groupId("g")
                .artifactId("a")
                .version("1")
                .pomPath(Path.of("a.pom"))
                .build();

        when(pomFinder.findPomForArtifact("g", "a")).thenReturn(coord);
        when(verifier.verifyArtifactExists(coord)).thenReturn(true);
        when(pomParser.parseDependencies(coord.getPomPath())).thenReturn(List.of());

        Injector injector = Guice.createInjector(
                Modules.override(new ServicesModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(PomFinder.class).toInstance(pomFinder);
                        bind(PomParser.class).toInstance(pomParser);
                        bind(ArtifactVerifier.class).toInstance(verifier);
                    }
                })
        );

        VersionScanner scanner = injector.getInstance(VersionScanner.class);
        Map<String, MavenCoordinate> result = scanner.scanSystemArtifacts(Set.of("g:a"));

        assertEquals(coord, result.get("g:a"));
        assertTrue(scanner.getNotFoundDependencies().isEmpty());
    }

    @Test
    @DisplayName("Finds plugin artifact via standard variants")
    void findsPluginArtifact() {
        MavenCoordinate coord = MavenCoordinate.builder()
                .groupId("com.acme.plugin")
                .artifactId("com.acme.plugin.gradle.plugin")
                .version("1")
                .pomPath(Path.of("p.pom"))
                .build();

        when(pomFinder.findPomForArtifact("com.acme.plugin", "com.acme.plugin.gradle.plugin")).thenReturn(coord);
        when(verifier.verifyArtifactExists(coord)).thenReturn(true);

        Injector injector = Guice.createInjector(
                Modules.override(new ServicesModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(PomFinder.class).toInstance(pomFinder);
                        bind(PomParser.class).toInstance(pomParser);
                        bind(ArtifactVerifier.class).toInstance(verifier);
                    }
                })
        );

        VersionScanner scanner = injector.getInstance(VersionScanner.class);
        MavenCoordinate result = scanner.findPluginArtifact("com.acme.plugin");

        assertEquals(coord, result);
    }
}
