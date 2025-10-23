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
package org.altlinux.xgradle;

import org.altlinux.xgradle.cli.CliArgumentsContainer;

import java.util.List;

/**
 * Configuration class for the XGradle tool.
 * Provides access to tool configuration parameters from command-line arguments.
 *
 * @author Ivan Khanas
 */
public class ToolConfig {
    private final CliArgumentsContainer arguments;

    /**
     * Constructs a new ToolConfig with the specified arguments' container.
     *
     * @param arguments container for command-line arguments
     */
    public ToolConfig(CliArgumentsContainer arguments) {
        this.arguments = arguments;
    }

    /**
     * Gets the list of excluded artifacts.
     *
     * @return list of artifact patterns to exclude
     */
    public List<String> getExcludedArtifacts() {
        return arguments.getExcludedArtifact();
    }

    /**
     * Checks if snapshot artifacts are allowed.
     *
     * @return true if snapshot artifacts are allowed, false otherwise
     */
    public boolean isAllowSnapshots() {
        return arguments.hasAllowSnapshots();
    }

    /**
     * Gets the list of POM files for which to remove parent blocks.
     *
     * @return list of POM patterns for parent removal
     */
    public List<String> getRemoveParentPoms() {
        return arguments.getRemoveParentPoms();
    }

    /**
     * Gets the installation prefix to strip from paths.
     *
     * @return install prefix string or null if not specified
     */
    public String getInstallPrefix() {
        return arguments.getInstallPrefix();
    }
}