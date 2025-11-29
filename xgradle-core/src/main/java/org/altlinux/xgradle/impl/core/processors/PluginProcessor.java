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
package org.altlinux.xgradle.impl.core.processors;

import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.services.PomFinder;
import org.altlinux.xgradle.impl.services.VersionScanner;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.plugin.management.PluginResolveDetails;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.altlinux.xgradle.impl.utils.ui.Painter.green;

/**
 * Processes plugin dependency resolution with support for BOM (Bill of Materials) packages.
 *
 * <p>This class implements the core logic for resolving Gradle plugins from locally available
 * artifacts, with special handling for BOM-based plugin packages. It works by:</p>
 * <ul>
 *   <li>Intercepting plugin resolution requests through Gradle's plugin management system</li>
 *   <li>Scanning local repositories for available plugin artifacts using a {@link VersionScanner}</li>
 *   <li>Handling regular plugin artifacts by mapping them to Maven coordinates</li>
 *   <li>Processing BOM packages by recursively resolving their contained dependencies</li>
 *   <li>Preventing duplicate processing of BOMs through tracking</li>
 * </ul>
 *
 * <p>The processor skips core Gradle plugins (those starting with "org.gradle.") and simple
 * plugin IDs without dots, as these are typically resolved through Gradle's built-in mechanisms.</p>
 *
 * @see VersionScanner
 * @see PomFinder
 *
 * @author Ivan Khanas
 */
public class PluginProcessor {
    private final VersionScanner versionScanner;
    private final PomFinder pomFinder;
    private final Logger logger;
    private final Set<String> processedBoms = new HashSet<>();

    /**
     * Constructs a new PluginProcessor with the necessary services.
     *
     * @param versionScanner the scanner used to find plugin artifacts in local repositories
     * @param pomFinder the finder used to locate and parse POM files for dependency information
     * @param logger the logger instance for reporting resolution activities
     */
    public PluginProcessor(VersionScanner versionScanner, PomFinder pomFinder, Logger logger) {
        this.versionScanner = versionScanner;
        this.pomFinder = pomFinder;
        this.logger = logger;
    }

    /**
     * Configures the plugin resolution strategy for the given Gradle settings.
     *
     * <p>This method sets up a resolution strategy that intercepts all plugin requests
     * and delegates them to the {@link #processPlugin(PluginResolveDetails)} method
     * for custom processing.</p>
     *
     * @param settings the Gradle settings to configure
     */
    public void configurePluginResolution(Settings settings) {
        settings.getPluginManagement().getResolutionStrategy().eachPlugin(this::processPlugin);
    }

    /**
     * Processes an individual plugin resolution request.
     *
     * <p>This method handles plugin resolution by:
     * <ol>
     *   <li>Skipping core Gradle plugins and simple plugin IDs</li>
     *   <li>Locating the corresponding artifact using the {@link VersionScanner}</li>
     *   <li>Handling regular plugins by applying their coordinates</li>
     *   <li>Processing BOM packages by resolving their contained dependencies</li>
     * </ol>
     *
     * @param requested the plugin resolution details containing the requested plugin ID
     */
    private void processPlugin(PluginResolveDetails requested) {
        String pluginId = requested.getRequested().getId().getId();
        if (pluginId.startsWith("org.gradle.") || !pluginId.contains(".")) {
            logger.lifecycle("Skipping core plugin: {}", pluginId);
            return;
        }

        MavenCoordinate coord = versionScanner.findPluginArtifact(pluginId, logger);
        if (coord != null && coord.isValid()) {
            if (coord.isBom()) {
                processBomPlugin(coord, requested);
            } else {
                usePlugin(requested, coord);
            }
        } else {
            logger.warn("Plugin not resolved: {}", pluginId);
        }
    }


    /**
     * Processes a BOM plugin package by resolving its contained dependencies.
     *
     * <p>This method:
     * <ol>
     *   <li>Checks if the BOM has already been processed to prevent cycles</li>
     *   <li>Parses the BOM's POM file to extract dependency information</li>
     *   <li>Recursively processes each dependency (handling nested BOMs)</li>
     *   <li>Applies any non-BOM dependencies as plugins</li>
     * </ol>
     *
     * @param bomCoord the Maven coordinates of the BOM package
     * @param requested the original plugin resolution request
     */
    private void processBomPlugin(MavenCoordinate bomCoord, PluginResolveDetails requested) {
        String bomKey = bomCoord.getGroupId() + ":" + bomCoord.getArtifactId() + ":" + bomCoord.getVersion();
        if (processedBoms.contains(bomKey)) {
            return;
        }
        processedBoms.add(bomKey);

        List<MavenCoordinate> dependencies = pomFinder.getPomParser()
                .parseDependencies(bomCoord.getPomPath(), logger);

        for (MavenCoordinate dep : dependencies) {
            if (dep.isBom()) {
                processBomPlugin(dep, requested);
            } else {
                usePlugin(requested, dep);
            }
        }
    }

    /**
     * Applies a plugin resolution using the given Maven coordinates.
     *
     * <p>This method configures the plugin resolution to use the specific artifact
     * identified by the provided coordinates, overriding any other resolution
     * mechanisms that might be in place.</p>
     *
     * @param requested the plugin resolution details to modify
     * @param coord the Maven coordinates of the plugin artifact to use
     */
    private void usePlugin(PluginResolveDetails requested, MavenCoordinate coord) {
        String module = coord.getGroupId() + ":" + coord.getArtifactId() + ":" + coord.getVersion();
        requested.useModule(module);
        requested.useVersion(coord.getVersion());
        logger.lifecycle(green("Resolved plugin: {} -> {}"), requested.getRequested().getId().getId(), module);
    }
}