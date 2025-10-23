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
package org.altlinux.xgradle.api.processors;

import java.util.List;
import java.util.Optional;

/**
 * Generic interface for POM file processors.
 * Defines contract for processing POM files and retrieving artifact coordinates.
 *
 * @param <R> the return type of processed results
 * @author Ivan Khanas
 */
public interface PomProcessor<R> {

    /**
     * Retrieves POM files from the specified directory, optionally filtered by artifact names.
     *
     * @param searchingDir the directory to search for POM files
     * @param artifactName optional list of artifact names to filter by
     * @return processed results of type R
     */
    R pomsFromDirectory(String searchingDir, Optional<List<String>> artifactName);
}
