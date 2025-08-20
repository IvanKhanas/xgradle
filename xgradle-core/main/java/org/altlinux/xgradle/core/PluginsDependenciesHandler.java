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
package org.altlinux.xgradle.core;

import org.altlinux.xgradle.core.managers.RepositoryManager;
import org.altlinux.xgradle.extensions.SystemDepsExtension;
import org.altlinux.xgradle.model.MavenCoordinate;
import org.altlinux.xgradle.services.VersionScanner;
import org.altlinux.xgradle.services.*;

import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.File;

import static org.altlinux.xgradle.utils.Painter.green;

/**
 * Handles plugin dependencies by adding local flat directory repositories
 * and configuring plugin resolution strategy based on found plugin artifacts.
 *
 * <p>This class scans the configured system jars directory, adds it as a flatDir repository,
 * and intercepts plugin resolution requests to provide specific plugin versions
 * based on local Maven coordinates discovered via a VersionScanner.</p>
 *
 * @author Ivan Khanas
 */
public class PluginsDependenciesHandler {
    private static final Logger logger = Logging.getLogger(PluginsDependenciesHandler.class);
    private final RepositoryManager pluginsRepository;
    private final VersionScanner versionScanner;

    /**
     * Constructs a PluginsDependenciesHandler with default components:
     * - a VersionScanner that uses PomFinder with DefaultPomParser
     * - a FileSystemArtifactVerifier
     */
    public PluginsDependenciesHandler() {
        this.pluginsRepository = new RepositoryManager(logger);
        this.versionScanner = new VersionScanner(
                new PomFinder(new DefaultPomParser()),
                new FileSystemArtifactVerifier()
        );
    }

    /**
     * Initializes the plugin dependencies handling process.
     * <ul>
     *   <li>Checks the system jars directory availability.</li>
     *   <li>Adds the directory and its subdirectories as a flatDir repository to plugin management.</li>
     *   <li>Sets up a plugin resolution strategy to resolve plugins from local artifacts.</li>
     * </ul>
     *
     * @param settings Gradle Settings object for configuration
     */
    public void handle(Settings settings) {
        File baseDir = new File(SystemDepsExtension.getJarsPath());
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            logger.warn("System jars directory unavailable: {}", baseDir.getAbsolutePath());
            return;
        }
        pluginsRepository.configurePluginsRepository(settings, baseDir);
        configurePluginResolution(settings);
    }

    /**
     * Configures the plugin resolution strategy in the given settings.
     * Resolves plugin requests by looking up the plugin artifact in the local repository.
     * Core Gradle plugins and plugins without group identifiers are skipped.
     *
     * @param settings Gradle Settings object
     */
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
                logger.lifecycle(green("Resolved plugin: {} -> {}"), pluginId, module);
            } else {
                logger.warn("Plugin not resolved: {}", pluginId);
            }
        });
    }
}