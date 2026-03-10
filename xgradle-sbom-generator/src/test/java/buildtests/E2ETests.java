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
package buildtests;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import testsupport.GradleTestKitSupport;
import testsupport.GradleTestKitSupport.ResolvedEnvironment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * End-to-end build tests for SBOM generation with the Gradle plugin.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@DisplayName("End-to-end SBOM build tests")
public class E2ETests {

    private ResolvedEnvironment testEnvironment;

    @BeforeEach
    public void prepareTestEnvironment() {
        testEnvironment = GradleTestKitSupport.resolveEnvironment();
    }

    @Test
    @DisplayName("Build with plugins only")
    public void testBuildWithOnlyPlugins(@TempDir File tempDir) throws IOException {
        runAndVerifyBuild("../buildExamples/testBuildWithOnlyPlugins", "cyclonedx", tempDir);
    }

    @Test
    @DisplayName("Build with dependencies only")
    public void testBuildWithOnlyDeps(@TempDir File tempDir) throws IOException {
        runAndVerifyBuild("../buildExamples/testBuildWithOnlyDeps", "spdx", tempDir);
    }

    @Test
    @DisplayName("Build with plugins and dependencies")
    public void testMixedBuild(@TempDir File tempDir) throws IOException {
        runAndVerifyBuild("../buildExamples/testMixedBuild", "cyclonedx", tempDir);
    }

    @Test
    @DisplayName("Multi-module build")
    public void multiModularTest(@TempDir File tempDir) throws IOException {
        runAndVerifyBuild("../buildExamples/multiModularTest", "spdx", tempDir);
    }

    private void runAndVerifyBuild(
            String projectPath,
            String format,
            File tempDir
    ) throws IOException {
        File gradleUserHome = GradleTestKitSupport.createGradleUserHome(tempDir);
        File pluginsDir = GradleTestKitSupport.preparePluginsDirectory(
                gradleUserHome,
                testEnvironment.getPluginJar()
        );
        File initScript = GradleTestKitSupport.writeInitScript(tempDir, pluginsDir);

        File testProjectDir = new File(tempDir, "testProject");
        GradleTestKitSupport.copyDirectory(Path.of(projectPath), testProjectDir.toPath());
        String testLibAbsolutePath = GradleTestKitSupport.copyTestLibsToProject(
                testEnvironment.getTestLibDir(),
                testProjectDir.toPath()
        );

        BuildResult result = GradleTestKitSupport.runOfflineBuild(
                testProjectDir,
                gradleUserHome,
                initScript,
                testLibAbsolutePath,
                format
        );

        assertEquals(TaskOutcome.SUCCESS, Objects.requireNonNull(result.task(":build")).getOutcome());

        File sbomReport = testProjectDir.toPath()
                .resolve("build")
                .resolve("reports")
                .resolve("xgradle")
                .resolve("sbom-" + format + ".json")
                .toFile();

        assertTrue(sbomReport.isFile(), "Expected SBOM report to be generated: " + sbomReport);

        String content = Files.readString(sbomReport.toPath());
        if ("spdx".equals(format)) {
            assertTrue(content.contains("\"spdxVersion\": \"SPDX-2.3\""));
            assertTrue(content.contains("\"SPDXID\": \"SPDXRef-DOCUMENT\""));
        } else {
            assertTrue(content.contains("\"bomFormat\": \"CycloneDX\""));
            assertTrue(content.contains("\"specVersion\": \"1.5\""));
        }

        assertExpectedSbomParameters(projectPath, format, content);
    }

    private void assertExpectedSbomParameters(
            String projectPath,
            String format,
            String content
    ) throws IOException {
        if ("spdx".equals(format)) {
            assertExpectedSpdxParameters(projectPath, content);
            return;
        }
        assertExpectedCycloneDxParameters(projectPath, content);
    }

    private void assertExpectedSpdxParameters(
            String projectPath,
            String content
    ) throws IOException {
        JsonObject document = JsonParser.parseString(content).getAsJsonObject();
        JsonArray packages = document.getAsJsonArray("packages");
        assertNotNull(packages, "SPDX packages array is missing");

        String commonsCliVersion = readTestLibPomVersion("commons-cli", "commons-cli.pom");
        String commonsIoVersion = readTestLibPomVersion("commons-io", "commons-io.pom");

        if (projectPath.contains("testBuildWithOnlyDeps")) {
            assertSpdxPackage(packages, "commons-cli:commons-cli", commonsCliVersion);
            assertSpdxPackage(packages, "commons-io:commons-io", commonsIoVersion);
            return;
        }

        if (projectPath.contains("multiModularTest")) {
            String gsonVersion = readTestLibPomVersion("google-gson", "gson.pom");
            String shadowVersion = readTestLibPomVersion("shadow-gradle-plugin", "shadow-gradle-plugin.pom");

            assertSpdxPackage(packages, "commons-cli:commons-cli", commonsCliVersion);
            assertSpdxPackage(packages, "commons-io:commons-io", commonsIoVersion);
            assertSpdxPackage(packages, "com.google.code.gson:gson", gsonVersion);
            assertSpdxPackage(packages, "com.gradleup.shadow:shadow-gradle-plugin", shadowVersion);
        }
    }

    private void assertExpectedCycloneDxParameters(
            String projectPath,
            String content
    ) throws IOException {
        JsonObject document = JsonParser.parseString(content).getAsJsonObject();
        JsonArray components = document.getAsJsonArray("components");
        assertNotNull(components, "CycloneDX components array is missing");

        String shadowVersion = readTestLibPomVersion("shadow-gradle-plugin", "shadow-gradle-plugin.pom");

        if (projectPath.contains("testBuildWithOnlyPlugins")) {
            JsonObject shadow = assertCycloneDxComponent(
                    components,
                    "com.gradleup.shadow",
                    "shadow-gradle-plugin",
                    shadowVersion,
                    "framework"
            );
            assertCycloneDxProperty(shadow, "xgradle:component-kind", "gradle-plugin");
            return;
        }

        if (projectPath.contains("testMixedBuild")) {
            String commonsCliVersion = readTestLibPomVersion("commons-cli", "commons-cli.pom");
            String commonsIoVersion = readTestLibPomVersion("commons-io", "commons-io.pom");

            assertCycloneDxComponent(
                    components,
                    "commons-cli",
                    "commons-cli",
                    commonsCliVersion,
                    "library"
            );
            assertCycloneDxComponent(
                    components,
                    "commons-io",
                    "commons-io",
                    commonsIoVersion,
                    "library"
            );
            JsonObject shadow = assertCycloneDxComponent(
                    components,
                    "com.gradleup.shadow",
                    "shadow-gradle-plugin",
                    shadowVersion,
                    "framework"
            );
            assertCycloneDxProperty(shadow, "xgradle:component-kind", "gradle-plugin");
        }
    }

    private void assertSpdxPackage(
            JsonArray packages,
            String expectedName,
            String expectedVersion
    ) {
        for (JsonElement entry : packages) {
            JsonObject packageObject = entry.getAsJsonObject();
            String name = getString(packageObject, "name");
            String version = getString(packageObject, "versionInfo");
            if (expectedName.equals(name) && expectedVersion.equals(version)) {
                return;
            }
        }

        fail("Expected SPDX package not found: " + expectedName + ":" + expectedVersion);
    }

    private JsonObject assertCycloneDxComponent(
            JsonArray components,
            String expectedGroup,
            String expectedName,
            String expectedVersion,
            String expectedType
    ) {
        for (JsonElement entry : components) {
            JsonObject component = entry.getAsJsonObject();
            String group = getString(component, "group");
            String name = getString(component, "name");
            if (!expectedGroup.equals(group) || !expectedName.equals(name)) {
                continue;
            }

            assertEquals(expectedVersion, getString(component, "version"));
            assertEquals(expectedType, getString(component, "type"));
            assertEquals(
                    "pkg:maven/" + expectedGroup + "/" + expectedName + "@" + expectedVersion,
                    getString(component, "purl")
            );
            return component;
        }

        fail("Expected CycloneDX component not found: " + expectedGroup + ":" + expectedName);
        return new JsonObject();
    }

    private void assertCycloneDxProperty(
            JsonObject component,
            String propertyName,
            String propertyValue
    ) {
        JsonArray properties = component.getAsJsonArray("properties");
        assertNotNull(properties, "CycloneDX properties array is missing");

        for (JsonElement entry : properties) {
            JsonObject property = entry.getAsJsonObject();
            if (propertyName.equals(getString(property, "name"))
                    && propertyValue.equals(getString(property, "value"))) {
                return;
            }
        }

        fail("Expected CycloneDX property not found: " + propertyName + "=" + propertyValue);
    }

    private String getString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            return null;
        }
        return value.getAsString();
    }

    private String readPomVersion(Path pomPath) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(pomPath.toFile());

            XPath xPath = XPathFactory.newInstance().newXPath();
            String version = normalize(
                    xPath.evaluate("/*[local-name()='project']/*[local-name()='version']/text()", document)
            );
            if (version != null) {
                return version;
            }

            String parentVersion = normalize(
                    xPath.evaluate(
                            "/*[local-name()='project']/*[local-name()='parent']/*[local-name()='version']/text()",
                            document
                    )
            );
            if (parentVersion != null) {
                return parentVersion;
            }
        } catch (Exception e) {
            throw new IOException("Failed to read POM version from " + pomPath, e);
        }

        throw new IOException("POM version is missing: " + pomPath);
    }

    private String readTestLibPomVersion(
            String directory,
            String fileName
    ) throws IOException {
        return readPomVersion(
                testEnvironment.getTestLibDir().toPath().resolve(directory).resolve(fileName)
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
