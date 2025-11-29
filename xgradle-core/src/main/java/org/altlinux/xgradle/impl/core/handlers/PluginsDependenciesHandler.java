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
package org.altlinux.xgradle.impl.core.handlers;

import org.altlinux.xgradle.impl.core.managers.PluginManager;

import org.altlinux.xgradle.impl.extensions.SystemDepsExtension;
import org.altlinux.xgradle.impl.services.DefaultPomParser;
import org.altlinux.xgradle.impl.services.FileSystemArtifactVerifier;
import org.altlinux.xgradle.impl.services.PomFinder;
import org.altlinux.xgradle.impl.services.VersionScanner;

import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

/**
 * Central handler for managing plugin dependencies in Gradle builds.
 *
 * <p>This class serves as the main entry point for configuring plugin dependency resolution
 * using locally available artifacts. It orchestrates the process of:</p>
 * <ul>
 *   <li>Scanning configured system directories for available plugin artifacts</li>
 *   <li>Adding local directories as flat repository sources for plugin resolution</li>
 *   <li>Intercepting plugin resolution requests to provide specific versions from local artifacts</li>
 *   <li>Handling both regular plugins and BOM (Bill of Materials) based plugin packages</li>
 * </ul>
 *
 * <p>The handler uses a {@link PluginManager} to delegate the actual plugin management operations,
 * providing a clean separation of concerns while ensuring consistent plugin resolution behavior
 * across all projects in the build.</p>
 *
 * <p><strong>Note:</strong> This handler specifically focuses on Gradle plugins and should not
 * be confused with regular project dependency management.</p>
 *
 * @see PluginManager
 * @see VersionScanner
 * @see SystemDepsExtension#getJarsPath()
 *
 * @author Ivan Khanas
 */
public class PluginsDependenciesHandler {
    private static final Logger logger = Logging.getLogger(PluginsDependenciesHandler.class);
    private final PluginManager pluginManager;

    /**
     * Constructs a new PluginsDependenciesHandler with default service implementations.
     *
     * <p>Initializes the handler with:
     * <ul>
     *   <li>A {@link VersionScanner} configured with {@link PomFinder} and {@link FileSystemArtifactVerifier}</li>
     *   <li>A {@link PluginManager} to handle plugin resolution operations</li>
     * </ul>
     * </p>
     */
    public PluginsDependenciesHandler() {
        PomFinder pomFinder = new PomFinder(new DefaultPomParser());
        VersionScanner versionScanner = new VersionScanner(pomFinder, new FileSystemArtifactVerifier());
        this.pluginManager = new PluginManager(versionScanner, pomFinder, logger);
    }

    /**
     * Initializes and configures plugin dependency resolution for the given Gradle settings.
     *
     * <p>This method:
     * <ol>
     *   <li>Validates the existence of the system jars directory</li>
     *   <li>Adds the directory and its subdirectories as flatDir repositories to plugin management</li>
     *   <li>Configures a plugin resolution strategy to resolve plugins from local artifacts</li>
     *   <li>Handles both regular plugins and BOM-based plugin packages</li>
     * </ol>
     *
     * <p>If the system jars directory does not exist or is not accessible, a warning is logged
     * but the build continues with standard plugin resolution mechanisms.</p>
     *
     * @param settings the Gradle Settings object used to configure plugin management
     */
    public void handle(Settings settings) {
        pluginManager.handle(settings);
    }
}