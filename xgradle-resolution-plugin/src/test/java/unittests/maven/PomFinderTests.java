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
package unittests.maven;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.altlinux.xgradle.impl.maven.MavenModule;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.interfaces.indexing.PomIndex;
import org.altlinux.xgradle.interfaces.maven.PomFinder;
import org.gradle.api.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PomFinder contract")
class PomFinderTests {

    @Mock
    private PomIndex pomIndex;

    @Mock
    private Logger logger;

    @Test
    @DisplayName("Delegates to PomIndex and builds on construction")
    void delegatesToIndex(@TempDir Path tempDir) {
        MavenCoordinate coord = MavenCoordinate.builder()
                .groupId("g")
                .artifactId("a")
                .version("1")
                .build();

        when(pomIndex.find("g", "a")).thenReturn(Optional.of(coord));
        when(pomIndex.findAllForGroup("g")).thenReturn(List.of(coord));

        String prev = System.getProperty("maven.poms.dir");
        System.setProperty("maven.poms.dir", tempDir.toString());
        try {
            Injector injector = Guice.createInjector(
                    Modules.override(new MavenModule()).with(new AbstractModule() {
                        @Override
                        protected void configure() {
                            bind(PomIndex.class).toInstance(pomIndex);
                            bind(Logger.class).toInstance(logger);
                        }
                    })
            );

            PomFinder finder = injector.getInstance(PomFinder.class);

            verify(pomIndex).build(Path.of(tempDir.toString()));
            assertEquals(coord, finder.findPomForArtifact("g", "a"));
            assertEquals(List.of(coord), finder.findAllPomsForGroup("g"));
        } finally {
            if (prev != null) {
                System.setProperty("maven.poms.dir", prev);
            } else {
                System.clearProperty("maven.poms.dir");
            }
        }
    }
}
