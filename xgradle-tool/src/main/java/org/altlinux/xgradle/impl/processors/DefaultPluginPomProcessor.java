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
package org.altlinux.xgradle.impl.processors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.impl.bindingannotations.processingtypes.GradlePlugin;
import org.altlinux.xgradle.impl.config.ToolConfig;
import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.altlinux.xgradle.interfaces.processors.PomProcessor;
import org.altlinux.xgradle.interfaces.services.PomService;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;
import java.util.List;

/**
 * Default implementation of PomProcessor for Gradle plugin artifacts.
 * Implements {@link PomProcessor<HashMap<String} and {@link Path>>}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultPluginPomProcessor implements PomProcessor<HashMap<String, Path>> {

    private final PomParser<HashMap<String, Path>> pluginsParser;
    private final PomService pomService;
    private final ToolConfig toolConfig;

    @Inject
    DefaultPluginPomProcessor(
            @GradlePlugin PomParser<HashMap<String, Path>> pluginsParser,
            PomService pomService,
            ToolConfig toolConfig
    ) {
        this.pluginsParser = pluginsParser;
        this.pomService = pomService;
        this.toolConfig = toolConfig;
    }

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
