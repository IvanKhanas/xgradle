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
package org.altlinux.xgradle.api.collectors;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Interface for POM file collection operations.
 * Defines contract for collecting POM files from directories.
 *
 * @author Ivan Khanas
 */
public interface PomCollector {

    /**
     * Collects all POM files from the specified directory and its subdirectories.
     *
     * @param searchingDir the directory to search for POM files
     * @return set of all found POM file paths
     */
    Set<Path> collectAll(String searchingDir);

    /**
     * Collects selected POM files from the specified directory based on artifact names.
     *
     * @param searchingDir the directory to search for POM files
     * @param artifactName list of artifact names to filter by
     * @return set of filtered POM file paths
     */
    Set<Path> collectSelected(String searchingDir, List<String> artifactName);
}