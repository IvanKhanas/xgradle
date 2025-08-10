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
 * See the License  for the specific language governing permissions and
 * limitations under the License.
 */
package org.altlinux.gradlePlugin.core;

import org.altlinux.gradlePlugin.core.managers.RepositoryManager;
import org.altlinux.gradlePlugin.extensions.SystemDepsExtension;
import org.altlinux.gradlePlugin.model.MavenCoordinate;
import org.altlinux.gradlePlugin.services.VersionScanner;
import org.altlinux.gradlePlugin.services.*;

import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.File;

import static org.altlinux.gradlePlugin.utils.Painter.green;

public class PluginsDependenciesHandler {
    private static final Logger logger = Logging.getLogger(PluginsDependenciesHandler.class);
    private final RepositoryManager pluginsRepository;
    private final VersionScanner versionScanner;

    public PluginsDependenciesHandler() {
        this.pluginsRepository = new RepositoryManager(logger);
        this.versionScanner = new VersionScanner(
                new PomFinder(new DefaultPomParser()),
                new FileSystemArtifactVerifier()
        );
    }

    public void handle(Settings settings) {
        File baseDir = new File(SystemDepsExtension.getJarsPath());
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            logger.warn("System jars directory unavailable: {}", baseDir.getAbsolutePath());
            return;
        }
        pluginsRepository.configurePluginsRepository(settings, baseDir);
        configurePluginResolution(settings);
    }

    private void configurePluginResolution(Settings settings) {
        settings.getPluginManagement().getResolutionStrategy().eachPlugin(requested -> {
            String pluginId = requested.getRequested().getId().getId();
            if (pluginId.startsWith("org.gradle.") || !pluginId.contains(".")) {
                logger.lifecycle("Skipping core plugin: {}", pluginId);
                return;
            }

            MavenCoordinate coord = versionScanner.findPluginArtifact(pluginId, logger);
            if (coord != null && coord.isValid()) {
                String module = coord.getGroupId() + ":" + coord.getArtifactId() + ":" + coord.getVersion();
                requested.useModule(module);
                requested.useVersion(coord.getVersion());
                logger.lifecycle(green("Resolved plugin: {} → {}"), pluginId, module);
            } else {
                logger.warn("Plugin not resolved: {}", pluginId);
            }
        });
    }
}