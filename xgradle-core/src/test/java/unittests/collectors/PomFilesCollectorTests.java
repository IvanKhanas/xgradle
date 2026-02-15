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
package unittests.collectors;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

import org.altlinux.xgradle.interfaces.collectors.PomFilesCollector;
import org.altlinux.xgradle.impl.collectors.CollectorsModule;
import org.gradle.api.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PomFilesCollector contract")
class PomFilesCollectorTests {

    @Mock
    private Logger logger;

    private PomFilesCollector collector;

    @BeforeEach
    void setUp() {
        Injector injector = Guice.createInjector(
                Modules.override(new CollectorsModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(Logger.class).toInstance(logger);
                    }
                })
        );

        collector = injector.getInstance(PomFilesCollector.class);
    }

    @Test
    @DisplayName("Collects pom files under root directory")
    void collectsPomFiles(@TempDir Path tempDir) throws IOException {
        Path pom = Files.writeString(tempDir.resolve("a.pom"), "<project/>");
        Files.writeString(tempDir.resolve("a.txt"), "skip");
        Path nested = Files.createDirectories(tempDir.resolve("nested"));
        Path nestedPom = Files.writeString(nested.resolve("b.pom"), "<project/>");

        List<Path> result = collector.collect(tempDir);

        assertEquals(2, result.size());
        assertEquals(Set.of(pom, nestedPom), new HashSet<>(result));
    }

    @Test
    @DisplayName("Invalid root directory logs and returns empty list")
    void invalidRootLogsAndReturnsEmpty(@TempDir Path tempDir) throws IOException {
        Path file = Files.writeString(tempDir.resolve("not-dir.txt"), "x");

        List<Path> result = collector.collect(file);

        assertTrue(result.isEmpty());
        verify(logger).lifecycle("POM root directory is not valid: {}", file);
    }
}
