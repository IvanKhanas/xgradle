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
package org.altlinux.xgradle.core.managers;

import org.altlinux.xgradle.core.PluginsDependenciesHandler;
import org.altlinux.xgradle.core.processors.PluginProcessor;
import org.altlinux.xgradle.extensions.SystemDepsExtension;
import org.altlinux.xgradle.services.PomFinder;
import org.altlinux.xgradle.services.VersionScanner;

import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;

import java.io.File;

/**
 * Manages the configuration of plugin resolution for Gradle builds.
 *
 * <p>This class coordinates the setup of plugin dependency resolution by:
 * <ul>
 *   <li>Configuring local repository sources for plugin resolution</li>
 *   <li>Setting up custom plugin resolution strategies</li>
 *   <li>Delegating detailed plugin processing to a {@link PluginProcessor}</li>
 * </ul>
 *
 * <p>The manager works in conjunction with the {@link PluginsDependenciesHandler}
 * to provide a complete solution for resolving plugins from local artifacts rather
 * than from remote repositories.</p>
 *
 * @author Ivan Khanas
 * @see RepositoryManager
 * @see PluginProcessor
 * @see SystemDepsExtension#getJarsPath()
 */
public class PluginManager {
    private final RepositoryManager repositoryManager;
    private final PluginProcessor pluginProcessor;
    private final Logger logger;

    /**
     * Constructs a new PluginManager with the necessary services.
     *
     * @param versionScanner the scanner used to find plugin artifacts
     * @param pomFinder the finder used to locate and parse POM files
     * @param logger the logger instance for reporting configuration activities
     */
    public PluginManager(VersionScanner versionScanner, PomFinder pomFinder, Logger logger) {
        this.repositoryManager = new RepositoryManager(logger);
        this.pluginProcessor = new PluginProcessor(versionScanner, pomFinder, logger);
        this.logger = logger;
    }

    /**
     * Configures plugin dependency resolution for the given Gradle settings.
     *
     * <p>This method:
     * <ol>
     *   <li>Validates the existence of the system jars directory</li>
     *   <li>Configures the directory as a repository for plugin resolution</li>
     *   <li>Sets up the custom plugin resolution strategy</li>
     * </ol>
     *
     * <p>If the system jars directory does not exist or is not accessible,
     * a warning is logged but the build continues with standard resolution.</p>
     *
     * @param settings the Gradle settings to configure
     */
    public void handle(Settings settings) {
        File baseDir = new File(SystemDepsExtension.getJarsPath());

        if (baseDir.exists() & baseDir.isDirectory()) {
            repositoryManager.configurePluginsRepository(settings, baseDir);
            pluginProcessor.configurePluginResolution(settings);
        }else {
            logger.warn("System jars directory does not exist or is not a directory {}", baseDir);
        }
    }
}
