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
import org.altlinux.xgradle.interfaces.parsers.PomParser;
import org.altlinux.xgradle.interfaces.processors.PluginProcessor;
import org.altlinux.xgradle.interfaces.services.VersionScanner;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.plugin.management.PluginResolveDetails;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.altlinux.xgradle.impl.utils.logging.LogPainter.green;

/**
 * Processes plugin dependency resolution with support for BOM (Bill of Materials) packages.
 * Implements {@link PluginProcessor}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultPluginProcessor implements PluginProcessor {

    private final VersionScanner versionScanner;
    private final Logger logger;
    private final PomParser pomParser;

    @Inject
    DefaultPluginProcessor(VersionScanner versionScanner, PomParser pomParser, Logger logger) {
        this.versionScanner = versionScanner;
        this.pomParser = pomParser;
        this.logger = logger;
    }

    @Override
    public void process(Settings settings) {
        Set<String> processedBoms = new HashSet<>();
        settings.getPluginManagement()
                .getResolutionStrategy()
                .eachPlugin(requested -> processPlugin(requested, processedBoms));
    }

    private void processPlugin(PluginResolveDetails requested, Set<String> processedBoms) {
        String pluginId = requested.getRequested().getId().getId();
        if (pluginId.startsWith("org.gradle.") || !pluginId.contains(".")) {
            logger.lifecycle("Skipping core plugin: {}", pluginId);
            return;
        }

        MavenCoordinate coord = versionScanner.findPluginArtifact(pluginId);
        if (coord != null && coord.isValid()) {
            if (coord.isBom()) {
                processBomPlugin(coord, requested, processedBoms);
            } else {
                usePlugin(requested, coord);
            }
        } else {
            logger.warn("Plugin not resolved: {}", pluginId);
        }
    }


    private void processBomPlugin(
            MavenCoordinate bomCoord,
            PluginResolveDetails requested,
            Set<String> processedBoms
    ) {
        String bomKey = bomCoord.getGroupId() + ":" + bomCoord.getArtifactId() + ":" + bomCoord.getVersion();
        if (processedBoms.contains(bomKey)) {
            return;
        }
        processedBoms.add(bomKey);

        List<MavenCoordinate> dependencies = pomParser
                .parseDependencies(bomCoord.getPomPath());

        for (MavenCoordinate dep : dependencies) {
            if (dep.isBom()) {
                processBomPlugin(dep, requested, processedBoms);
            } else {
                usePlugin(requested, dep);
            }
        }
    }

    private void usePlugin(PluginResolveDetails requested, MavenCoordinate coord) {
        String module = coord.getGroupId() + ":" + coord.getArtifactId() + ":" + coord.getVersion();
        requested.useModule(module);
        requested.useVersion(coord.getVersion());
        logger.lifecycle(green("Resolved plugin: {} -> {}"), requested.getRequested().getId().getId(), module);
    }
}
