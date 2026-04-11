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
package org.altlinux.xgradle.impl.generators;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.impl.enums.SbomFormat;
import org.altlinux.xgradle.impl.models.SbomComponent;
import org.altlinux.xgradle.impl.validation.SbomValidationUtils;
import org.altlinux.xgradle.interfaces.builders.SbomDocumentBuilder;
import org.altlinux.xgradle.interfaces.generators.SbomGenerator;
import org.altlinux.xgradle.interfaces.preprocessors.SbomComponentPreprocessor;
import org.altlinux.xgradle.interfaces.writers.SbomOutputWriter;

import java.nio.file.Path;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link SbomGenerator}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultSbomGenerator implements SbomGenerator {

    private final Map<SbomFormat, SbomDocumentBuilder> buildersByFormat;
    private final SbomComponentPreprocessor componentPreprocessor;
    private final SbomOutputWriter sbomOutputWriter;

    @Inject
    DefaultSbomGenerator(
            Set<SbomDocumentBuilder> sbomDocumentBuilders,
            SbomComponentPreprocessor componentPreprocessor,
            SbomOutputWriter sbomOutputWriter
    ) {
        this.buildersByFormat = indexBuilders(sbomDocumentBuilders);
        this.componentPreprocessor = componentPreprocessor;
        this.sbomOutputWriter = sbomOutputWriter;
    }

    @Override
    public void generate(
            SbomFormat format,
            Path outputPath,
            String projectName,
            String projectVersion,
            Collection<SbomComponent> components
    ) {
        SbomValidationUtils.requireNonNull(format, "SBOM format must not be null");
        SbomValidationUtils.requireOutputPath(outputPath);

        String normalizedProjectName = SbomValidationUtils.requireProjectNameOrDefault(projectName);
        String normalizedProjectVersion = SbomValidationUtils.requireProjectVersionOrDefault(projectVersion);
        List<SbomComponent> orderedComponents = componentPreprocessor.preprocess(components);
        JsonObject report = resolveBuilder(format).build(
                normalizedProjectName,
                normalizedProjectVersion,
                orderedComponents
        );

        sbomOutputWriter.write(outputPath, report);
    }

    private Map<SbomFormat, SbomDocumentBuilder> indexBuilders(Set<SbomDocumentBuilder> builders) {
        Map<SbomFormat, SbomDocumentBuilder> indexedBuilders = new EnumMap<>(SbomFormat.class);
        builders.stream()
                .filter(builder -> builder != null)
                .forEach(builder -> {
                    SbomDocumentBuilder previous = indexedBuilders.put(builder.format(), builder);
                    if (previous != null) {
                        throw new IllegalStateException(
                                "Duplicate SBOM document builder for format: " + builder.format()
                        );
                    }
                });

        return Map.copyOf(indexedBuilders);
    }

    private SbomDocumentBuilder resolveBuilder(SbomFormat format) {
        SbomDocumentBuilder builder = buildersByFormat.get(format);
        if (builder == null) {
            throw new IllegalStateException("No SBOM builder registered for format: " + format);
        }
        return builder;
    }
}
