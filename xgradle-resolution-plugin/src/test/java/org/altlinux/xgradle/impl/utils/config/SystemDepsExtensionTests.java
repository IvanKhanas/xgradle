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
package org.altlinux.xgradle.impl.utils.config;

import org.altlinux.xgradle.impl.extensions.SystemDepsExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("SystemDepsExtension contract")
class SystemDepsExtensionTests {

    @TempDir
    Path tempDir;

    private String prevHome;
    private String prevJavaLib;

    @BeforeEach
    void setUp() throws Exception {
        prevHome = System.getProperty("user.home");
        prevJavaLib = System.getProperty("java.library.dir");

        XGradleConfig.resetForTests();
        System.setProperty("user.home", tempDir.toString());
        Files.createDirectories(tempDir.resolve(".xgradle"));
    }

    @AfterEach
    void tearDown() {
        restoreProperty("user.home", prevHome);
        restoreProperty("java.library.dir", prevJavaLib);
        XGradleConfig.resetForTests();
    }

    @Test
    @DisplayName("Prefers system property over config")
    void prefersSystemProperty() throws Exception {
        writeConfig("java.library.dir=/tmp/from-config");
        System.setProperty("java.library.dir", "/tmp/from-system");

        assertEquals("/tmp/from-system", SystemDepsExtension.getJarsPath());
    }

    @Test
    @DisplayName("Reads config when system property is missing")
    void readsConfigWhenSystemMissing() throws Exception {
        writeConfig("java.library.dir=/tmp/from-config");
        System.clearProperty("java.library.dir");

        assertEquals("/tmp/from-config", SystemDepsExtension.getJarsPath());
    }

    @Test
    @DisplayName("Parses single path from system property")
    void parsesSinglePath() {
        System.setProperty("java.library.dir", " /tmp/jars ");

        List<File> paths = SystemDepsExtension.getJarsPaths();
        assertEquals(List.of(new File("/tmp/jars")), paths);
    }

    @Test
    @DisplayName("Parses multiple paths from system property")
    void parsesMultiplePaths() {
        System.setProperty("java.library.dir", " /tmp/jars1 , /tmp/jars2 ");

        List<File> paths = SystemDepsExtension.getJarsPaths();
        assertEquals(List.of(new File("/tmp/jars1"), new File("/tmp/jars2")), paths);
    }

    @Test
    @DisplayName("Parses multiple paths from config")
    void parsesMultiplePathsFromConfig() throws Exception {
        writeConfig("java.library.dir=/tmp/jars1, /tmp/jars2");
        System.clearProperty("java.library.dir");

        List<File> paths = SystemDepsExtension.getJarsPaths();
        assertEquals(List.of(new File("/tmp/jars1"), new File("/tmp/jars2")), paths);
    }

    @Test
    @DisplayName("Returns null/empty when properties are missing")
    void returnsEmptyWhenMissing() {
        System.clearProperty("java.library.dir");
        System.clearProperty("maven.poms.dir");

        assertNull(SystemDepsExtension.getJarsPath());
        assertEquals(List.of(), SystemDepsExtension.getJarsPaths());
        assertNull(SystemDepsExtension.getPomsPath());
    }

    @Test
    @DisplayName("Prefers system property over config for POMs path")
    void prefersSystemPropertyForPoms() throws Exception {
        writeConfig("maven.poms.dir=/tmp/poms-config");
        System.setProperty("maven.poms.dir", "/tmp/poms-system");

        assertEquals("/tmp/poms-system", SystemDepsExtension.getPomsPath());
    }

    @Test
    @DisplayName("Reads POMs path from config when system property is missing")
    void readsPomsConfigWhenSystemMissing() throws Exception {
        writeConfig("maven.poms.dir=/tmp/poms-config");
        System.clearProperty("maven.poms.dir");

        assertEquals("/tmp/poms-config", SystemDepsExtension.getPomsPath());
    }

    private void writeConfig(String content) throws Exception {
        Path config = tempDir.resolve(".xgradle").resolve("xgradle.config");
        Files.writeString(config, content);
        XGradleConfig.resetForTests();
    }

    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
