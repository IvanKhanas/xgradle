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
package org.altlinux.xgradle.impl.managers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.managers.PluginManager;
import org.altlinux.xgradle.interfaces.managers.RepositoryManager;
import org.altlinux.xgradle.interfaces.processors.PluginProcessor;
import org.altlinux.xgradle.impl.extensions.SystemDepsExtension;

import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages the configuration of plugin resolution for Gradle builds.
 * Implements {@link PluginManager}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultPluginManager  implements PluginManager {

    private final RepositoryManager repositoryManager;
    private final PluginProcessor pluginProcessor;
    private final Logger logger;

    @Inject
    DefaultPluginManager(RepositoryManager repositoryManager, PluginProcessor pluginProcessor, Logger logger) {
        this.repositoryManager = repositoryManager;
        this.pluginProcessor = pluginProcessor;
        this.logger = logger;
    }

    public void configure(Settings settings) {
        List<File> baseDirs = SystemDepsExtension.getJarsPaths();
        if (baseDirs.isEmpty()) {
            return;
        }

        List<File> validDirs = baseDirs.stream()
                .filter(dir -> dir != null && dir.isDirectory() && dir.canRead())
                .collect(Collectors.toList());

        if (validDirs.isEmpty()) {
            logger.warn("System jars directories do not exist or are not readable: {}", baseDirs);
            return;
        }

        if (validDirs.size() != baseDirs.size()) {
            List<File> invalidDirs = baseDirs.stream()
                    .filter(dir -> dir == null || !dir.isDirectory() || !dir.canRead())
                    .collect(Collectors.toList());
            logger.warn("Skipping invalid system jars directories: {}", invalidDirs);
        }

        repositoryManager.configurePluginsRepository(settings, validDirs);
        pluginProcessor.process(settings);
    }
}
