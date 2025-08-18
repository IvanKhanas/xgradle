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
package org.altlinux.gradlePlugin.plugin;

import org.altlinux.gradlePlugin.core.PluginsDependenciesHandler;
import org.altlinux.gradlePlugin.core.ProjectDependenciesHandler;
import org.altlinux.gradlePlugin.utils.LogoPrinter;

import org.gradle.api.Plugin;
import org.gradle.api.invocation.Gradle;

import org.jetbrains.annotations.NotNull;

/**
 * Class implements {@link Plugin} interface
 * <p>Core plugin implementation that applies to Gradle itself rather than individual projects.
 *
 * <p>This plugin provides:
 * <ul>
 *   <li>Custom dependency resolution for other Gradle plugins</li>
 *   <li>Access to all phases of the Gradle build lifecycle</li>
 * </ul>
 *
 * <p>Lifecycle hooks:
 * <ol>
 *   <li>{@code beforeSettings}: Handles plugin dependencies resolution</li>
 *   <li>{@code projectsLoaded}: Adds custom repository configuration</li>
 *   <li>{@code projectsEvaluated}: Processes project dependencies after configuration</li>
 * </ol>
 *
 * @author Ivan Khanas
 */
public class XGradlePlugin implements Plugin<Gradle> {

    /**
     * Applies the plugin to the Gradle instance.
     *
     * <p>Main operations:
     * <ol>
     *   <li>Prints the plugin logo if enabled</li>
     *   <li>Initializes dependency handlers</li>
     *   <li>Registers lifecycle hooks:
     *     <ul>
     *       <li>Before settings: Processes plugin dependencies</li>
     *       <li>Projects loaded: Configures repositories</li>
     *       <li>Projects evaluated: Processes project dependencies</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * @param gradle the Gradle instance to which the plugin is applied
     */
    @Override
    public void apply(@NotNull Gradle gradle) {
        if (LogoPrinter.isLogoEnabled()) {
            LogoPrinter.printCenteredBanner();
        }

        PluginsDependenciesHandler pluginsHandler = new PluginsDependenciesHandler();
        ProjectDependenciesHandler projectHandler = new ProjectDependenciesHandler();

        gradle.beforeSettings(pluginsHandler::handle);
        gradle.projectsLoaded(projectHandler::addRepository);
        gradle.projectsEvaluated(projectHandler::handleAfterConfiguration);
    }
}
