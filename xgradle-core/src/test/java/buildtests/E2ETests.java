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

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end build tests for the Gradle plugin.
 *
 * @author Ivan Khanas
 */
@DisplayName("End-to-end build tests")
public class E2ETests {

    private File pluginJar;

    @BeforeEach
    public void preparePluginJar() {
        pluginJar = new File("build/dist/xgradle-core.jar");
        if (!pluginJar.exists()) {
            throw new IllegalStateException("Could not find plugin jar: " + pluginJar.getAbsolutePath());
        }
    }

    @Test
    @DisplayName("Build with plugins only")
    public void testBuildWithOnlyPlugins(@TempDir File tempDir) throws IOException {
        runAndVerifyBuild("../buildExamples/testBuildWithOnlyPlugins", tempDir);
    }

    @Test
    @DisplayName("Build with dependencies only")
    public void testBuildWithOnlyDeps(@TempDir File tempDir) throws IOException {
        runAndVerifyBuild("../buildExamples/testBuildWithOnlyDeps", tempDir);
    }

    @Test
    @DisplayName("Build with plugins and dependencies")
    public void testMixedBuild(@TempDir File tempDir) throws IOException {
        runAndVerifyBuild("../buildExamples/testMixedBuild", tempDir);
    }

    @Test
    @DisplayName("Multi-module build")
    public void multiModularTest(@TempDir File tempDir) throws IOException {
        runAndVerifyBuild("../buildExamples/multiModularTest", tempDir);
    }

    private void runAndVerifyBuild(String projectPath, File tempDir) throws IOException {
        File gradleUserHome = new File(tempDir, "gradleUserHome");
        File pluginsDir = new File(gradleUserHome, "lib/plugins");
        assertTrue(pluginsDir.mkdirs());

        File pluginTarget = new File(pluginsDir, pluginJar.getName());
        Files.copy(pluginJar.toPath(), pluginTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);

        File initScript = new File(tempDir, "xgradle-plugin.gradle");

        String template = loadResource("xgradle-plugin-test.gradle");

        String initScriptContent = template
                .replace("{{PLUGINS_DIR}}", pluginsDir.getAbsolutePath()
                        .replace("\\", "\\\\"));

        Files.writeString(initScript.toPath(), initScriptContent);

        File testProjectDir = new File(tempDir, "testProject");
        copyDirectory(Path.of(projectPath), testProjectDir.toPath());

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments(
                        "--gradle-user-home", gradleUserHome.getAbsolutePath(),
                        "--init-script", initScript.getAbsolutePath(),
                        "build",
                        "--offline"
                )
                .forwardOutput()
                 .build();

        System.out.println(result.getOutput());

        assertEquals(TaskOutcome.SUCCESS, Objects.requireNonNull(result.task(":build"))
                .getOutcome());
    }

    private String loadResource(String resourceName) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) throw new IOException("Resource not found: " + resourceName);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

     private void copyDirectory(Path sourceDir, Path targetDir) throws IOException {
        if (!Files.exists(targetDir)) Files.createDirectories(targetDir);
        Files.walk(sourceDir).forEach(source -> {
            try {
                Path target = targetDir.resolve(sourceDir.relativize(source));
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
