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
package org.altlinux.gradlePlugin.core;

import org.altlinux.gradlePlugin.extensions.SystemDepsExtension;
import org.altlinux.gradlePlugin.model.MavenCoordinate;
import org.altlinux.gradlePlugin.services.DefaultPomParser;
import org.altlinux.gradlePlugin.services.FileSystemArtifactVerifier;
import org.altlinux.gradlePlugin.services.PomFinder;

import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.altlinux.gradlePlugin.utils.Painter.green;

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
    private final VersionScanner versionScanner;

    /**
     * Constructs a PluginsDependenciesHandler with default components:
     * - a VersionScanner that uses PomFinder with DefaultPomParser
     * - a FileSystemArtifactVerifier
     */
    public PluginsDependenciesHandler() {
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
        logger.lifecycle("Initializing plugins dependencies handler");
        File libDir = new File(SystemDepsExtension.getJarsPath());
        if (!libDir.exists() || !libDir.isDirectory()) {
            logger.warn("System jars directory unavailable: {}", libDir.getAbsolutePath());
            return;
        }
        addRepository(settings, libDir);
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
                String module = coord.groupId + ":" + coord.artifactId + ":" + coord.version;
                requested.useModule(module);
                requested.useVersion(coord.version);
                logger.lifecycle(green("Resolved plugin: {} → {}"), pluginId, module);
            } else {
                logger.warn("Plugin not resolved: {}", pluginId);
            }
        });
    }

    /**
     * Adds a flat directory repository to the plugin management repositories,
     * including all subdirectories (up to 3 levels deep) under the given base directory.
     * The repository is given a unique name for identification.
     *
     * @param settings Gradle Settings object
     * @param libDir the base directory containing plugin jars
     */
    private void addRepository(Settings settings, File libDir) {
        settings.getPluginManagement().getRepositories().flatDir(repo -> {
            String repoName = "SystemPluginsRepo-" + UUID.randomUUID();
            repo.setName(repoName);
            List<File> allDirs = new ArrayList<>(List.of(libDir));

            try {
                Files.walk(libDir.toPath(), 3)
                        .filter(Files::isDirectory)
                        .filter(path -> !path.equals(libDir.toPath()))
                        .forEach(path -> allDirs.add(path.toFile()));
            } catch (Exception e) {
                logger.error("Directory scan error: {}", e.getMessage());
            }

            allDirs.forEach(repo::dir);
            logger.lifecycle("Repository '{}' with {} directories", repoName, allDirs.size());
        });
    }
}