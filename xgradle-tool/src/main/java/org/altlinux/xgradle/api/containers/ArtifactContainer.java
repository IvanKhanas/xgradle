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
package org.altlinux.xgradle.api.containers;

import org.altlinux.xgradle.ProcessingType;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.List;

/**
 * Interface for artifact container operations.
 * Defines contract for managing collections of artifacts with different access patterns.
 *
 * @author Ivan Khanas
 */
public interface ArtifactContainer {

    /**
     * Retrieves artifacts from the specified directory based on processing type.
     *
     * @param searchingDir the directory to search for artifacts
     * @param artifactName optional list of artifact names to filter by
     * @param processingType the type of processing (LIBRARY or PLUGINS)
     * @return map of artifact signatures to file paths
     */
    HashMap<String, Path> getArtifacts(String searchingDir, Optional<List<String>> artifactName, ProcessingType processingType);

    /**
     * Retrieves artifact file paths from the specified directory based on processing type.
     *
     * @param searchingDir the directory to search for artifacts
     * @param artifactName optional list of artifact names to filter by
     * @param processingType the type of processing (LIBRARY or PLUGINS)
     * @return collection of artifact file paths
     */
    Collection<Path> getArtifactPaths(String searchingDir, Optional<List<String>> artifactName, ProcessingType processingType);

    /**
     * Retrieves artifact signatures from the specified directory based on processing type.
     *
     * @param searchingDir the directory to search for artifacts
     * @param artifactName optional list of artifact names to filter by
     * @param processingType the type of processing (LIBRARY or PLUGINS)
     * @return collection of artifact signatures
     */
    Collection<String> getArtifactSignatures(String searchingDir, Optional<List<String>> artifactName, ProcessingType processingType);
}
