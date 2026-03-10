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
package unittests.writers;

import com.google.gson.JsonObject;
import org.altlinux.xgradle.interfaces.writers.SbomOutputWriter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import unittests.AbstractSbomModuleTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SBOM output writer")
class SbomOutputWriterTests extends AbstractSbomModuleTest {

    @TempDir
    Path tempDir;

    private final SbomOutputWriter writer = injector.getInstance(SbomOutputWriter.class);

    @Test
    @DisplayName("Creates parent directories and writes JSON")
    void createsParentDirectoriesAndWritesJson() throws Exception {
        Path target = tempDir.resolve("nested/reports/sbom.json");
        JsonObject report = new JsonObject();
        report.addProperty("text", "<xml>");

        writer.write(target, report);

        assertTrue(Files.exists(target));
        String content = Files.readString(target);
        assertTrue(content.contains("\"text\": \"<xml>\""));
    }

    @Test
    @DisplayName("Overwrites existing report file")
    void overwritesExistingReportFile() throws Exception {
        Path target = tempDir.resolve("sbom.json");

        JsonObject first = new JsonObject();
        first.addProperty("value", "first");
        writer.write(target, first);

        JsonObject second = new JsonObject();
        second.addProperty("value", "second");
        writer.write(target, second);

        String content = Files.readString(target);
        assertTrue(content.contains("\"value\": \"second\""));
        assertFalse(content.contains("\"value\": \"first\""));
    }

    @Test
    @DisplayName("Wraps IO errors in runtime exception")
    void wrapsIoErrorsInRuntimeException() throws Exception {
        Path targetDir = tempDir.resolve("sbom-as-dir");
        Files.createDirectories(targetDir);

        JsonObject report = new JsonObject();
        report.addProperty("value", "any");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> writer.write(targetDir, report)
        );

        assertTrue(exception.getMessage().contains("Failed to write SBOM"));
        assertNotNull(exception.getCause());
    }
}
