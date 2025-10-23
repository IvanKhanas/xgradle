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
import java.util.HashMap;
import java.util.Optional;
import java.util.List;

/**
 * Default implementation of PomProcessor for Gradle plugin artifacts.
 * Handles processing of Gradle plugin POM files with artifact exclusion
 * and snapshot filtering capabilities.
 * Specifically designed for Gradle plugin installation scenarios.
 *
 * @author Ivan Khanas
 */
@Singleton
public class DefaultPluginPomProcessor implements PomProcessor<HashMap<String, Path>> {
    private final PomParser<HashMap<String, Path>> pluginsParser;
    private final PomService pomService;
    private final ToolConfig toolConfig;

    /**
     * Constructs a new DefaultPluginPomProcessor with required dependencies.
     *
     * @param pluginsParser the parser for plugin POM files
     * @param pomService the service for POM processing operations
     * @param toolConfig the configuration for the tool
     */
    @Inject
    public DefaultPluginPomProcessor(
            @Named("gradlePlugins") PomParser<HashMap<String, Path>> pluginsParser,
            PomService pomService,
            ToolConfig toolConfig
    ) {
        this.pluginsParser = pluginsParser;
        this.pomService = pomService;
        this.toolConfig = toolConfig;
    }

    /**
     * Processes plugin artifacts from the specified directory.
     * Applies artifact exclusion and snapshot filtering for Gradle plugins.
     *
     * @param searchingDir the directory to search for plugin artifacts
     * @param artifactNames optional list of artifact names to filter by
     * @return map of POM file paths to corresponding JAR file paths
     */
    @Override
    public HashMap<String, Path> pomsFromDirectory(String searchingDir, Optional<List<String>> artifactNames) {
        HashMap<String, Path> artifacts = pluginsParser.getArtifactCoords(searchingDir, artifactNames);
        artifacts = pomService.excludeArtifacts(toolConfig.getExcludedArtifacts(), artifacts);

        if (!toolConfig.isAllowSnapshots()) {
            artifacts = pomService.excludeSnapshots(artifacts);
        }

        return artifacts;
    }
}