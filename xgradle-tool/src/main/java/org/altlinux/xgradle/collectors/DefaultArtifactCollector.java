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
package org.altlinux.xgradle.collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.altlinux.xgradle.ProcessingType;
import org.altlinux.xgradle.api.collectors.ArtifactCollector;
import org.altlinux.xgradle.api.processors.PomProcessor;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of ArtifactCollector for collecting artifacts based on processing type.
 * Supports library, plugin and javadoc artifact collection.
 *
 * @author Ivan Khanas
 */
@Singleton
public class DefaultArtifactCollector implements ArtifactCollector {
    private final PomProcessor<HashMap<String, Path>> libraryPomProcessor;
    private final PomProcessor<HashMap<String, Path>> gradlePlugins;
    private final PomProcessor<HashMap<String, Path>> javadocProcessor;

    /**
     * Constructs a new DefaultArtifactCollector with required dependencies.
     *
     * @param libraryPomProcessor processor for library POM files
     * @param gradlePlugins processor for Gradle plugin POM files
     * @param javadocProcessor processor for Javadoc files
     */
    @Inject
    public DefaultArtifactCollector(
            @Named("Library") PomProcessor<HashMap<String, Path>> libraryPomProcessor,
            @Named("gradlePlugins") PomProcessor<HashMap<String, Path>> gradlePlugins,
            @Named("Javadoc") PomProcessor<HashMap<String, Path>> javadocProcessor
    ) {
        this.libraryPomProcessor = libraryPomProcessor;
        this.gradlePlugins = gradlePlugins;
        this.javadocProcessor = javadocProcessor;
    }

    /**
     * Collects artifacts from the specified directory based on processing type.
     *
     * @param searchingDir the directory to search for artifacts
     * @param artifactName optional list of artifact names to filter by
     * @param processingType the type of processing (LIBRARY, PLUGINS or JAVADOC)
     * @return map of artifact coordinates to file paths
     */
    @Override
    public HashMap<String,Path> collect(String searchingDir, Optional<List<String>> artifactName, ProcessingType processingType) {
        switch (processingType) {
            case PLUGINS:
                return gradlePlugins.pomsFromDirectory(searchingDir, artifactName);
            case JAVADOC:
                return javadocProcessor.pomsFromDirectory(searchingDir, artifactName);
            case LIBRARY:
            default:
                return libraryPomProcessor.pomsFromDirectory(searchingDir, artifactName);
        }
    }
}