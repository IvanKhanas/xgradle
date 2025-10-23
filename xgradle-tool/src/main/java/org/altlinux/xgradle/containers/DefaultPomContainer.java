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

import org.altlinux.xgradle.api.collectors.PomCollector;
import org.altlinux.xgradle.api.containers.PomContainer;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of PomContainer for managing POM file collections.
 * Provides access to all POM files or selected POM files based on artifact names.
 *
 * @author Ivan Khanas
 */
@Singleton
public class DefaultPomContainer implements PomContainer {
    private final PomCollector pomCollector;

    /**
     * Constructs a new DefaultPomContainer with required dependencies.
     *
     * @param pomCollector collector for retrieving POM files
     */
    @Inject
    public DefaultPomContainer(PomCollector pomCollector) {
        this.pomCollector = pomCollector;
    }

    /**
     * Retrieves all POM files from the specified directory.
     *
     * @param searchingDir the directory to search for POM files
     * @return set of all POM file paths
     */
    @Override
    public Set<Path> getAllPoms(String searchingDir) {
        return pomCollector.collectAll(searchingDir);
    }

    /**
     * Retrieves selected POM files from the specified directory based on artifact names.
     *
     * @param searchingDir the directory to search for POM files
     * @param artifactName list of artifact names to filter by
     * @return set of filtered POM file paths
     */
    @Override
    public Set<Path> getSelectedPoms(String searchingDir, List<String> artifactName) {
        return pomCollector.collectSelected(searchingDir, artifactName);
    }
}