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
import org.altlinux.xgradle.interfaces.services.ArtifactVerifier;
import org.altlinux.xgradle.interfaces.services.VersionScanner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ArtifactVerifier contract")
class ArtifactVerifierTests {

    @Mock
    private VersionScanner versionScanner;

    @Test
    @DisplayName("Recognizes POM packaging without jar file")
    void pomPackagingAlwaysExists() {
        Injector injector = Guice.createInjector(
                Modules.override(new ServicesModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(VersionScanner.class).toInstance(versionScanner);
                    }
                })
        );
        ArtifactVerifier verifier = injector.getInstance(ArtifactVerifier.class);
        MavenCoordinate coord = MavenCoordinate.builder()
                .groupId("g")
                .artifactId("a")
                .version("1")
                .packaging("pom")
                .build();

        assertTrue(verifier.verifyArtifactExists(coord));
    }

    @Test
    @DisplayName("Finds jar files in system directory")
    void findsJarFile(@TempDir Path tempDir) throws Exception {
        String prev = System.getProperty("java.library.dir");
        System.setProperty("java.library.dir", tempDir.toString());
        try {
            Path jar = tempDir.resolve("lib-1.0.jar");
            Files.writeString(jar, "jar");

            Injector injector = Guice.createInjector(
                    Modules.override(new ServicesModule()).with(new AbstractModule() {
                        @Override
                        protected void configure() {
                            bind(VersionScanner.class).toInstance(versionScanner);
                        }
                    })
            );
            ArtifactVerifier verifier = injector.getInstance(ArtifactVerifier.class);
            MavenCoordinate coord = MavenCoordinate.builder()
                    .groupId("g")
                    .artifactId("lib")
                    .version("1.0")
                    .build();

            assertTrue(verifier.verifyArtifactExists(coord));
        } finally {
            if (prev != null) {
                System.setProperty("java.library.dir", prev);
            } else {
                System.clearProperty("java.library.dir");
            }
        }
    }
}
