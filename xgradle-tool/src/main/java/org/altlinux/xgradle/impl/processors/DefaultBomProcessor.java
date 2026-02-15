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

import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Bom;
import org.altlinux.xgradle.impl.config.ToolConfig;
import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.altlinux.xgradle.interfaces.processors.PomProcessor;
import org.altlinux.xgradle.interfaces.services.PomService;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.List;

/**
 * Default implementation of PomProcessor for BOM (Bill of Materials) artifacts.
 * Implements {@link PomProcessor}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultBomProcessor implements PomProcessor<Set<Path>> {

    private final PomParser<Set<Path>> pomParser;
    private final PomService pomService;
    private final ToolConfig toolConfig;

    @Inject
    DefaultBomProcessor(
            @Bom PomParser<Set<Path>> pomParser,
            PomService pomService,
            ToolConfig toolConfig
    ) {
        this.pomParser = pomParser;
        this.pomService = pomService;
        this.toolConfig = toolConfig;
    }

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

    private Set<Path> getArtifactsFromParser(String searchingDir, Optional<List<String>> artifactNames) {
        if (artifactNames.isPresent()) {
            return pomParser.getArtifactCoords(searchingDir, artifactNames);
        } else {
            return pomParser.getArtifactCoords(searchingDir, Optional.empty());
        }
    }
}
