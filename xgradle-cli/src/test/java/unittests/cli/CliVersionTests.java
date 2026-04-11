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
package unittests.cli;

import org.altlinux.xgradle.impl.cli.commands.CliVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("CliVersion contract")
class CliVersionTests {

    @Test
    @DisplayName("printVersion: reads application.properties and prints banner")
    void printVersionReadsBannerFromResources() {
        PrintStream original = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            assertDoesNotThrow(() -> new CliVersion().printVersion());
            String output = out.toString();
            assertTrue(output.contains("Version:"), "Banner should contain Version label");
            assertTrue(output.contains("Revision:"), "Banner should contain Revision label");
            assertTrue(output.contains("Build Time:"), "Banner should contain Build Time label");
        } finally {
            System.setOut(original);
        }
    }
}
