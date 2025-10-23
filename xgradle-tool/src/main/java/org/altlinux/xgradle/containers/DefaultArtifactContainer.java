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
package org.altlinux.xgradle.containers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.ProcessingType;
import org.altlinux.xgradle.api.collectors.ArtifactCollector;
import org.altlinux.xgradle.api.containers.ArtifactContainer;

import java.util.List;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

import java.nio.file.Path;

/**
 * Default implementation of ArtifactContainer for managing artifact collections.
 * Provides access to artifacts, their paths, and signatures based on processing type.
 *
 * @author Ivan Khanas
 */
@Singleton
public class DefaultArtifactContainer implements ArtifactContainer {
    private final ArtifactCollector artifactCollector;

    /**
     * Constructs a new DefaultArtifactContainer with required dependencies.
     *
     * @param artifactCollector collector for retrieving artifacts
     */
    @Inject
    public DefaultArtifactContainer(ArtifactCollector artifactCollector) {
        this.artifactCollector = artifactCollector;
    }

    /**
     * Retrieves artifacts from the specified directory based on processing type.
     *
     * @param searchingDirectory the directory to search for artifacts
     * @param artifactNames optional list of artifact names to filter by
     * @param processingType the type of processing (LIBRARY or PLUGINS)
     * @return map of artifact signatures to file paths
     */
    @Override
    public HashMap<String, Path> getArtifacts(String searchingDirectory, Optional<List<String>> artifactNames, ProcessingType processingType) {
        if (artifactNames.isPresent()) {
            return artifactCollector.collect(searchingDirectory, artifactNames, processingType);
        } else {
            return artifactCollector.collect(searchingDirectory, Optional.empty(), processingType);
        }
    }

    /**
     * Retrieves artifact file paths from the specified directory based on processing type.
     *
     * @param searchingDirectory the directory to search for artifacts
     * @param artifactNames optional list of artifact names to filter by
     * @param processingType the type of processing (LIBRARY or PLUGINS)
     * @return collection of artifact file paths
     */
    @Override
    public Collection<Path> getArtifactPaths(String searchingDirectory, Optional<List<String>> artifactNames, ProcessingType processingType) {
        if (artifactNames.isPresent()) {
            return artifactCollector.collect(searchingDirectory,artifactNames, processingType).values();
        } else {
            return artifactCollector.collect(searchingDirectory, Optional.empty(), processingType).values();
        }
    }

    /**
     * Retrieves artifact signatures from the specified directory based on processing type.
     *
     * @param searchingDirectory the directory to search for artifacts
     * @param artifactNames optional list of artifact names to filter by
     * @param processingType the type of processing (LIBRARY or PLUGINS)
     * @return collection of artifact signatures
     */
    @Override
    public Collection<String> getArtifactSignatures(String searchingDirectory, Optional<List<String>> artifactNames, ProcessingType processingType) {
        if (artifactNames.isPresent()) {
            return artifactCollector.collect(searchingDirectory, artifactNames, processingType).keySet();
        } else {
            return artifactCollector.collect(searchingDirectory, Optional.empty(), processingType).keySet();
        }
    }
}