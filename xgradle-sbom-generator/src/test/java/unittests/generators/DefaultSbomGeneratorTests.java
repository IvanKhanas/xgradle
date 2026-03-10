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
package unittests.generators;

import org.altlinux.xgradle.impl.enums.SbomFormat;
import org.altlinux.xgradle.impl.models.SbomComponent;
import org.altlinux.xgradle.impl.models.SbomLicense;
import org.altlinux.xgradle.interfaces.generators.SbomGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import unittests.AbstractSbomModuleTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@DisplayName("Default SBOM generator")
class DefaultSbomGeneratorTests extends AbstractSbomModuleTest {

    @TempDir
    Path tempDir;

    private final SbomGenerator generator = injector.getInstance(SbomGenerator.class);

    @Test
    @DisplayName("Generates SPDX JSON file")
    void generatesSpdx() throws Exception {
        Path target = tempDir.resolve("sbom-spdx.json");

        generator.generate(
                SbomFormat.SPDX,
                target,
                "demo-project",
                "1.0.0",
                List.of(
                        SbomComponent.maven(
                                "org.example",
                                "demo-lib",
                                "1.2.3",
                                "https://example.org/demo",
                                "https://github.com/example/demo",
                                List.of(
                                        new SbomLicense(
                                                "Apache-2.0",
                                                "https://www.apache.org/licenses/LICENSE-2.0"
                                        )
                                )
                        ),
                        SbomComponent.file("standalone.jar")
                )
        );

        String content = Files.readString(target);
        assertTrue(content.contains("\"spdxVersion\": \"SPDX-2.3\""));
        assertTrue(content.contains("\"name\": \"org.example:demo-lib\""));
        assertTrue(content.contains("\"licenseDeclared\": \"Apache-2.0\""));
        assertTrue(content.contains("\"homepage\": \"https://example.org/demo\""));
    }

    @Test
    @DisplayName("Generates CycloneDX JSON file")
    void generatesCycloneDx() throws Exception {
        Path target = tempDir.resolve("sbom-cyclonedx.json");

        generator.generate(
                SbomFormat.CYCLONEDX,
                target,
                "demo-project",
                "1.0.0",
                List.of(
                        SbomComponent.maven(
                                "org.example",
                                "demo-lib",
                                "1.2.3",
                                "https://example.org/demo",
                                "https://github.com/example/demo",
                                List.of(
                                        new SbomLicense(
                                                "Apache-2.0",
                                                "https://www.apache.org/licenses/LICENSE-2.0"
                                        )
                                )
                        ),
                        SbomComponent.mavenPlugin(
                                "org.beryx",
                                "badass-jar",
                                "2.0.0"
                        )
                )
        );

        String content = Files.readString(target);
        assertTrue(content.contains("\"bomFormat\": \"CycloneDX\""));
        assertTrue(content.contains("\"specVersion\": \"1.5\""));
        assertTrue(content.contains("\"name\": \"demo-lib\""));
        assertTrue(content.contains("\"externalReferences\""));
        assertTrue(content.contains("\"licenses\""));
        assertTrue(content.contains("\"name\": \"badass-jar\""));
        assertTrue(content.contains("\"type\": \"framework\""));
        assertTrue(content.contains("\"xgradle:component-kind\""));
        assertTrue(content.contains("\"value\": \"gradle-plugin\""));
    }
}
