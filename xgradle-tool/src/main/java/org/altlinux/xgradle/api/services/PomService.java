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
package org.altlinux.xgradle.api.services;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;


import java.util.List;

/**
 * Service interface for POM file processing operations.
 * Provides methods for artifact filtering, parent block removal, and snapshot exclusion.
 */
public interface PomService {

    /**
     * Excludes artifacts from HashMap based on exclusion patterns.
     *
     * @param excludedArtifacts list of artifact patterns to exclude
     * @param artifactCoordinatesMap map of artifact coordinates to filter
     * @return filtered map of artifact coordinates
     */
    HashMap<String, Path> excludeArtifacts(List<String> excludedArtifacts,
                                           HashMap<String, Path> artifactCoordinatesMap);

    /**
     * Excludes artifacts from Set based on exclusion patterns.
     *
     * @param excludedArtifacts list of artifact patterns to exclude
     * @param artifactCoords set of artifact paths to filter
     * @return filtered set of artifact paths
     */
    Set<Path> excludeArtifacts(List<String> excludedArtifacts, Set<Path> artifactCoords);

    /**
     * Removes parent blocks from artifacts in HashMap.
     *
     * @param artifacts map of artifacts to process
     * @param removeParentPoms list of POM patterns for which to remove parent blocks
     */
    void removeParentBlocks(HashMap<String, Path> artifacts, List<String> removeParentPoms);

    /**
     * Removes parent blocks from BOM files in Set.
     *
     * @param bomFiles set of BOM files to process
     * @param removeParentPoms list of POM patterns for which to remove parent blocks
     */
    void removeParentBlocks(Set<Path> bomFiles, List<String> removeParentPoms);

    /**
     * Excludes snapshot artifacts from Set.
     *
     * @param pomFiles set of POM files to filter
     * @return filtered set without snapshot artifacts
     */
    Set<Path> excludeSnapshots(Set<Path> pomFiles);

    /**
     * Excludes snapshot artifacts from HashMap.
     *
     * @param artifactsMap map of artifacts to filter
     * @return filtered map without snapshot artifacts
     */
    HashMap<String, Path> excludeSnapshots(HashMap<String, Path> artifactsMap);
}
