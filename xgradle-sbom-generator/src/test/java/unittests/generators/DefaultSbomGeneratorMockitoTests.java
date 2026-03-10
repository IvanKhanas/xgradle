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

import com.google.gson.JsonObject;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;
import org.altlinux.xgradle.impl.enums.SbomFormat;
import org.altlinux.xgradle.impl.generators.GeneratorsModule;
import org.altlinux.xgradle.impl.models.SbomComponent;
import org.altlinux.xgradle.interfaces.builders.SbomDocumentBuilder;
import org.altlinux.xgradle.interfaces.generators.SbomGenerator;
import org.altlinux.xgradle.interfaces.preprocessors.SbomComponentPreprocessor;
import org.altlinux.xgradle.interfaces.writers.SbomOutputWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Default SBOM generator interactions")
class DefaultSbomGeneratorMockitoTests {

    @Mock
    private SbomDocumentBuilder spdxBuilder;

    @Mock
    private SbomComponentPreprocessor componentPreprocessor;

    @Mock
    private SbomOutputWriter sbomOutputWriter;

    @Test
    @DisplayName("Delegates generation pipeline to preprocessor builder and writer")
    void delegatesGenerationPipelineToCollaborators() {
        SbomGenerator generator = createGeneratorWithMocks();
        clearInvocations(spdxBuilder, componentPreprocessor, sbomOutputWriter);

        Path outputPath = Path.of("build/reports/xgradle/sbom-spdx.json");
        List<SbomComponent> inputComponents = List.of(SbomComponent.maven("org.example", "demo-lib", "1.0.0"));
        List<SbomComponent> orderedComponents = List.of(SbomComponent.maven("org.example", "demo-lib", "1.0.0"));
        JsonObject report = new JsonObject();

        when(componentPreprocessor.preprocess(inputComponents)).thenReturn(orderedComponents);
        when(spdxBuilder.build("demo", "1.0.0", orderedComponents)).thenReturn(report);

        generator.generate(SbomFormat.SPDX, outputPath, "demo", "1.0.0", inputComponents);

        verify(componentPreprocessor).preprocess(inputComponents);
        verify(spdxBuilder).build("demo", "1.0.0", orderedComponents);
        verify(sbomOutputWriter).write(outputPath, report);
    }

    @Test
    @DisplayName("Passes normalized default project info to builder")
    void passesNormalizedDefaultProjectInfoToBuilder() {
        SbomGenerator generator = createGeneratorWithMocks();
        clearInvocations(spdxBuilder, componentPreprocessor, sbomOutputWriter);

        Path outputPath = Path.of("build/reports/xgradle/sbom-spdx.json");
        List<SbomComponent> orderedComponents = List.of();
        JsonObject report = new JsonObject();

        when(componentPreprocessor.preprocess(null)).thenReturn(orderedComponents);
        when(spdxBuilder.build(anyString(), anyString(), eq(orderedComponents))).thenReturn(report);

        generator.generate(SbomFormat.SPDX, outputPath, "   ", null, null);

        verify(spdxBuilder).build("xgradle-project", "unspecified", orderedComponents);
        verify(sbomOutputWriter).write(outputPath, report);
    }

    private SbomGenerator createGeneratorWithMocks() {
        when(spdxBuilder.format()).thenReturn(SbomFormat.SPDX);

        Injector injector = Guice.createInjector(
                new GeneratorsModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(SbomComponentPreprocessor.class).toInstance(componentPreprocessor);
                        bind(SbomOutputWriter.class).toInstance(sbomOutputWriter);

                        Multibinder<SbomDocumentBuilder> builders = Multibinder.newSetBinder(
                                binder(),
                                SbomDocumentBuilder.class
                        );
                        builders.addBinding().toInstance(spdxBuilder);
                    }
                }
        );

        return injector.getInstance(SbomGenerator.class);
    }
}
