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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("XGradleConfig contract")
class XGradleConfigTests {

    @TempDir
    Path tempDir;

    private String prevHome;
    private String prevJavaLib;
    private String prevMavenPoms;
    private String prevScanDepth;

    @BeforeEach
    void setUp() throws Exception {
        prevHome = System.getProperty("user.home");
        prevJavaLib = System.getProperty("java.library.dir");
        prevMavenPoms = System.getProperty("maven.poms.dir");
        prevScanDepth = System.getProperty("xgradle.scan.depth");

        XGradleConfig.resetForTests();
        System.setProperty("user.home", tempDir.toString());
        Files.createDirectories(tempDir.resolve(".xgradle"));
    }

    @AfterEach
    void tearDown() {
        restoreProperty("user.home", prevHome);
        restoreProperty("java.library.dir", prevJavaLib);
        restoreProperty("maven.poms.dir", prevMavenPoms);
        restoreProperty("xgradle.scan.depth", prevScanDepth);
        XGradleConfig.resetForTests();
    }

    @Test
    @DisplayName("getProperty returns config value when system property is missing")
    void getPropertyFallsBackToConfig() throws Exception {
        writeConfig("java.library.dir=/tmp/jars");
        System.clearProperty("java.library.dir");

        String value = XGradleConfig.getProperty("java.library.dir");
        assertEquals("/tmp/jars", value);
    }

    @Test
    @DisplayName("getProperty prefers system property over config")
    void getPropertyPrefersSystemProperty() throws Exception {
        writeConfig("java.library.dir=/tmp/jars");
        System.setProperty("java.library.dir", "/opt/jars");

        String value = XGradleConfig.getProperty("java.library.dir");
        assertEquals("/opt/jars", value);
    }

    @Test
    @DisplayName("initSystemProperties sets missing values from config")
    void initSystemPropertiesSetsMissingValues() throws Exception {
        writeConfig("maven.poms.dir=/tmp/poms");
        System.clearProperty("maven.poms.dir");

        XGradleConfig.initSystemProperties();
        assertEquals("/tmp/poms", System.getProperty("maven.poms.dir"));
    }

    @Test
    @DisplayName("getIntProperty parses integer and falls back to default")
    void getIntPropertyParsesAndDefaults() throws Exception {
        writeConfig("xgradle.scan.depth=5");
        System.clearProperty("xgradle.scan.depth");

        int value = XGradleConfig.getIntProperty("xgradle.scan.depth", 3);
        assertEquals(5, value);

        writeConfig("xgradle.scan.depth=bad");
        int fallback = XGradleConfig.getIntProperty("xgradle.scan.depth", 3);
        assertEquals(3, fallback);
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
