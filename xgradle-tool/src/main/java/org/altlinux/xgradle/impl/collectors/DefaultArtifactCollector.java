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
package org.altlinux.xgradle.impl.collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.impl.bindingannotations.processingtypes.GradlePlugin;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Javadoc;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Library;
import org.altlinux.xgradle.impl.enums.ProcessingType;
import org.altlinux.xgradle.interfaces.collectors.ArtifactCollector;
import org.altlinux.xgradle.interfaces.processors.PomProcessor;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of ArtifactCollector for collecting artifacts based on processing type.
 * Implements {@link ArtifactCollector}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultArtifactCollector implements ArtifactCollector {
    private final PomProcessor<HashMap<String, Path>> libraryPomProcessor;
    private final PomProcessor<HashMap<String, Path>> gradlePlugins;
    private final PomProcessor<HashMap<String, Path>> javadocProcessor;

    @Inject
    DefaultArtifactCollector(
            @Library PomProcessor<HashMap<String, Path>> libraryPomProcessor,
            @GradlePlugin PomProcessor<HashMap<String, Path>> gradlePlugins,
            @Javadoc PomProcessor<HashMap<String, Path>> javadocProcessor
    ) {
        this.libraryPomProcessor = libraryPomProcessor;
        this.gradlePlugins = gradlePlugins;
        this.javadocProcessor = javadocProcessor;
    }

    @Override
    public HashMap<String,Path> collect(String searchingDir, Optional<List<String>> artifactName, ProcessingType processingType) {

        Objects.requireNonNull(processingType, "Processing type can not be null!");

        switch (processingType) {
            case PLUGINS:
                return gradlePlugins.pomsFromDirectory(searchingDir, artifactName);
            case JAVADOC:
                return javadocProcessor.pomsFromDirectory(searchingDir, artifactName);
            case LIBRARY:
                return libraryPomProcessor.pomsFromDirectory(searchingDir, artifactName);
            default:
                throw new IllegalStateException("Unsupported processing type: " + processingType);
        }
    }
}
