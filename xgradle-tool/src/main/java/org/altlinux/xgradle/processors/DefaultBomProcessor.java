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
package org.altlinux.xgradle.processors;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.altlinux.xgradle.ToolConfig;
import org.altlinux.xgradle.api.parsers.PomParser;
import org.altlinux.xgradle.api.processors.PomProcessor;
import org.altlinux.xgradle.api.services.PomService;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.List;

/**
 * Default implementation of PomProcessor for BOM (Bill of Materials) artifacts.
 * Processes BOM POM files with packaging type "pom" and dependency management sections.
 * Provides artifact exclusion, parent block removal, and snapshot filtering for BOM files.
 *
 * @author Ivan Khanas
 */
@Singleton
public class DefaultBomProcessor implements PomProcessor<Set<Path>> {
    private final PomParser<Set<Path>> pomParser;
    private final PomService pomService;
    private final ToolConfig toolConfig;

    /**
     * Constructs a new DefaultBomProcessor with required dependencies.
     *
     * @param pomParser the parser for BOM POM files
     * @param pomService the service for POM processing operations
     * @param toolConfig the configuration for the tool
     */
    @Inject
    public DefaultBomProcessor(
            @Named("Bom") PomParser<Set<Path>> pomParser,
            PomService pomService,
            ToolConfig toolConfig
    ) {
        this.pomParser = pomParser;
        this.pomService = pomService;
        this.toolConfig = toolConfig;
    }

    /**
     * Processes BOM artifacts from the specified directory.
     * Applies artifact exclusion, parent block removal, and snapshot filtering.
     *
     * @param searchingDir the directory to search for BOM files
     * @param artifactNames optional list of artifact names to filter by
     * @return set of BOM file paths that match the criteria
     */
    @Override
    public Set<Path> pomsFromDirectory(String searchingDir, Optional<List<String>> artifactNames) {
        Set<Path> artifacts = getArtifactsFromParser(searchingDir, artifactNames);

        artifacts = pomService.excludeArtifacts(toolConfig.getExcludedArtifacts(), artifacts);
        pomService.removeParentBlocks(artifacts, toolConfig.getRemoveParentPoms());

        if (!toolConfig.isAllowSnapshots()) {
            artifacts = pomService.excludeSnapshots(artifacts);
        }

        return artifacts;
    }

    /**
     * Retrieves artifacts from parser based on whether artifact names are specified.
     * Delegates to appropriate parser method depending on the presence of artifact names.
     *
     * @param searchingDir the directory to search for artifacts
     * @param artifactNames optional list of artifact names to filter by
     * @return set of artifact paths
     */
    private Set<Path> getArtifactsFromParser(String searchingDir, Optional<List<String>> artifactNames) {
        if (artifactNames.isPresent()) {
            return pomParser.getArtifactCoords(searchingDir, artifactNames);
        } else {
            return pomParser.getArtifactCoords(searchingDir, Optional.empty());
        }
    }
}