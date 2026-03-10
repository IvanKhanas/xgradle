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
package integrationtests.sbomtests;

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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for SBOM generation through Gradle TestKit.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@DisplayName("SBOM generator integration tests")
public class SbomGeneratorIntegrationTests {

    private ResolvedEnvironment testEnvironment;

    @BeforeEach
    public void prepareTestEnvironment() {
        testEnvironment = GradleTestKitSupport.resolveEnvironment();
    }

    @Test
    @DisplayName("Generates SPDX report for minimal project")
    public void generatesSpdxReport(@TempDir File tempDir) throws IOException {
        runMinimalBuildAndVerifySbom(tempDir, "spdx", "sbom-spdx.json", "\"spdxVersion\": \"SPDX-2.3\"");
    }

    @Test
    @DisplayName("Generates CycloneDX report for minimal project")
    public void generatesCycloneDxReport(@TempDir File tempDir) throws IOException {
        runMinimalBuildAndVerifySbom(
                tempDir,
                "cyclonedx",
                "sbom-cyclonedx.json",
                "\"bomFormat\": \"CycloneDX\""
        );
    }

    @Test
    @DisplayName("Skips SBOM generation for unsupported format")
    public void skipsUnsupportedFormat(@TempDir File tempDir) throws IOException {
        BuildExecution execution = runMinimalBuild(tempDir, "xml");
        assertEquals(TaskOutcome.SUCCESS, Objects.requireNonNull(execution.result.task(":build")).getOutcome());

        Path reportDir = execution.projectDir.toPath()
                .resolve("build")
                .resolve("reports")
                .resolve("xgradle");

        if (!Files.exists(reportDir)) {
            return;
        }

        try (Stream<Path> stream = Files.list(reportDir)) {
            boolean hasSbom = stream.anyMatch(path -> path.getFileName().toString().startsWith("sbom-"));
            assertFalse(hasSbom, "SBOM files should not be generated for unsupported format");
        }
    }

    private void runMinimalBuildAndVerifySbom(
            File tempDir,
            String format,
            String reportName,
            String expectedMarker
    ) throws IOException {
        BuildExecution execution = runMinimalBuild(tempDir, format);
        assertEquals(TaskOutcome.SUCCESS, Objects.requireNonNull(execution.result.task(":build")).getOutcome());

        File report = execution.projectDir.toPath()
                .resolve("build")
                .resolve("reports")
                .resolve("xgradle")
                .resolve(reportName)
                .toFile();

        assertTrue(report.isFile(), "Expected report file to exist: " + report.getAbsolutePath());
        String content = Files.readString(report.toPath());
        assertTrue(content.contains(expectedMarker), "Expected marker not found in report: " + expectedMarker);
    }

    private BuildExecution runMinimalBuild(
            File tempDir,
            String format
    ) throws IOException {
        File gradleUserHome = GradleTestKitSupport.createGradleUserHome(tempDir);
        File pluginsDir = GradleTestKitSupport.preparePluginsDirectory(
                gradleUserHome,
                testEnvironment.getPluginJar()
        );
        File initScript = GradleTestKitSupport.writeInitScript(tempDir, pluginsDir);

        File testProjectDir = GradleTestKitSupport.createDirectory(new File(tempDir, "integrationProject"));
        String testLibAbsolutePath = GradleTestKitSupport.copyTestLibsToProject(
                testEnvironment.getTestLibDir(),
                testProjectDir.toPath()
        );

        Files.writeString(
                testProjectDir.toPath().resolve("settings.gradle"),
                "rootProject.name = 'sbom-generator-integration'"
        );

        String buildScript = "plugins {" + System.lineSeparator()
                + "    id 'java'" + System.lineSeparator()
                + "}" + System.lineSeparator()
                + System.lineSeparator()
                + "group = 'org.example'" + System.lineSeparator()
                + "version = '1.0.0'" + System.lineSeparator();

        Files.writeString(testProjectDir.toPath().resolve("build.gradle"), buildScript);
        createJavaSource(testProjectDir.toPath(), "org.example.App");

        BuildResult result = GradleTestKitSupport.runOfflineBuild(
                testProjectDir,
                gradleUserHome,
                initScript,
                testLibAbsolutePath,
                format
        );

        return new BuildExecution(testProjectDir, result);
    }

    private void createJavaSource(Path projectDir, String fqcn) throws IOException {
        int dotIndex = fqcn.lastIndexOf('.');
        String packageName = fqcn.substring(0, dotIndex);
        String className = fqcn.substring(dotIndex + 1);

        Path sourceDir = projectDir
                .resolve("src")
                .resolve("main")
                .resolve("java")
                .resolve(packageName.replace('.', File.separatorChar));

        Files.createDirectories(sourceDir);

        String source = "package " + packageName + ";" + System.lineSeparator()
                + "public class " + className + " { }" + System.lineSeparator();

        Files.writeString(sourceDir.resolve(className + ".java"), source);
    }

    private static final class BuildExecution {
        private final File projectDir;
        private final BuildResult result;

        private BuildExecution(File projectDir, BuildResult result) {
            this.projectDir = projectDir;
            this.result = result;
        }
    }
}
