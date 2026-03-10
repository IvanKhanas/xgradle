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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import com.google.inject.multibindings.Multibinder;
import org.altlinux.xgradle.impl.di.SbomModule;
import org.altlinux.xgradle.impl.generators.GeneratorsModule;
import org.altlinux.xgradle.impl.preprocessors.PreprocessorsModule;
import org.altlinux.xgradle.impl.writers.WritersModule;
import org.altlinux.xgradle.impl.enums.SbomFormat;
import org.altlinux.xgradle.impl.models.SbomComponent;
import org.altlinux.xgradle.interfaces.builders.SbomDocumentBuilder;
import org.altlinux.xgradle.interfaces.generators.SbomGenerator;
import org.altlinux.xgradle.interfaces.preprocessors.SbomComponentPreprocessor;
import org.altlinux.xgradle.interfaces.writers.SbomOutputWriter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import unittests.AbstractSbomModuleTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("SBOM generator contract")
class SbomGeneratorContractTests extends AbstractSbomModuleTest {

    @TempDir
    Path tempDir;

    private final SbomGenerator generator = injector.getInstance(SbomGenerator.class);

    @Test
    @DisplayName("Uses default project name and version for blank values")
    void usesDefaultProjectNameAndVersionForBlankValues() throws Exception {
        Path target = tempDir.resolve("default-spdx.json");

        generator.generate(
                SbomFormat.SPDX,
                target,
                "   ",
                null,
                List.of()
        );

        JsonObject document = JsonParser.parseString(Files.readString(target)).getAsJsonObject();
        assertEquals("xgradle-project-sbom", document.get("name").getAsString());

        JsonObject creationInfo = document.getAsJsonObject("creationInfo");
        JsonArray creators = creationInfo.getAsJsonArray("creators");
        assertTrue(creators.get(0).getAsString().contains("xgradle-sbom-generator unspecified"));
    }

    @Test
    @DisplayName("Rejects null format")
    void rejectsNullFormat() {
        assertThrows(
                NullPointerException.class,
                () -> generator.generate(
                        null,
                        tempDir.resolve("out.json"),
                        "demo",
                        "1.0",
                        List.of()
                )
        );
    }

    @Test
    @DisplayName("Rejects null output path")
    void rejectsNullOutputPath() {
        assertThrows(
                NullPointerException.class,
                () -> generator.generate(
                        SbomFormat.SPDX,
                        null,
                        "demo",
                        "1.0",
                        List.of()
                )
        );
    }

    @Test
    @DisplayName("Provides singleton scope for generator infrastructure")
    void providesSingletonScopeForGeneratorInfrastructure() {
        SbomGenerator generator2 = injector.getInstance(SbomGenerator.class);
        SbomComponentPreprocessor preprocessor1 = injector.getInstance(SbomComponentPreprocessor.class);
        SbomComponentPreprocessor preprocessor2 = injector.getInstance(SbomComponentPreprocessor.class);
        SbomOutputWriter writer1 = injector.getInstance(SbomOutputWriter.class);
        SbomOutputWriter writer2 = injector.getInstance(SbomOutputWriter.class);

        assertSame(generator, generator2);
        assertSame(preprocessor1, preprocessor2);
        assertSame(writer1, writer2);
    }

    @Test
    @DisplayName("Accepts null component collection and emits empty package list")
    void acceptsNullComponentCollection() throws Exception {
        Path target = tempDir.resolve("null-components.json");

        generator.generate(
                SbomFormat.SPDX,
                target,
                "demo",
                "1.0.0",
                null
        );

        JsonObject document = JsonParser.parseString(Files.readString(target)).getAsJsonObject();
        assertEquals(0, document.getAsJsonArray("packages").size());
    }

    @Test
    @DisplayName("Fails when no builder is registered for requested format")
    void failsWhenNoBuilderIsRegistered() {
        Injector injectorWithoutBuilders = Guice.createInjector(
                new GeneratorsModule(),
                new PreprocessorsModule(),
                new WritersModule(),
                new NoBuildersModule()
        );
        SbomGenerator generatorWithoutBuilders;
        try {
            generatorWithoutBuilders = injectorWithoutBuilders.getInstance(SbomGenerator.class);
        } catch (CreationException e) {
            throw new AssertionError("Empty builders set should be injectable", e);
        }

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> generatorWithoutBuilders.generate(
                        SbomFormat.SPDX,
                        tempDir.resolve("out.json"),
                        "demo",
                        "1.0",
                        List.of()
                )
        );
        assertTrue(exception.getMessage().contains("No SBOM builder registered for format: SPDX"));
    }

    @Test
    @DisplayName("Fails fast when duplicate builder is registered")
    void failsFastWhenDuplicateBuilderIsRegistered() {
        Injector duplicateInjector = Guice.createInjector(
                new SbomModule(),
                new DuplicateSpdxBuilderModule()
        );

        ProvisionException exception = assertThrows(
                ProvisionException.class,
                () -> duplicateInjector.getInstance(SbomGenerator.class)
        );
        assertTrue(exception.getMessage().contains("Duplicate SBOM document builder for format: SPDX"));
    }

    private static final class DuplicateSpdxBuilderModule extends AbstractModule {

        @Override
        protected void configure() {
            Multibinder<SbomDocumentBuilder> binder = Multibinder.newSetBinder(
                    binder(),
                    SbomDocumentBuilder.class
            );
            binder.addBinding().to(FakeSpdxBuilder.class);
        }
    }

    private static final class NoBuildersModule extends AbstractModule {

        @Override
        protected void configure() {
            Multibinder.newSetBinder(binder(), SbomDocumentBuilder.class);
        }
    }

    private static final class FakeSpdxBuilder implements SbomDocumentBuilder {

        @Override
        public SbomFormat format() {
            return SbomFormat.SPDX;
        }

        @Override
        public JsonObject build(
                String projectName,
                String projectVersion,
                List<SbomComponent> components
        ) {
            JsonObject payload = new JsonObject();
            payload.addProperty("name", projectName);
            payload.addProperty("version", projectVersion);
            payload.addProperty("components", components != null ? components.size() : 0);
            return payload;
        }
    }
}
