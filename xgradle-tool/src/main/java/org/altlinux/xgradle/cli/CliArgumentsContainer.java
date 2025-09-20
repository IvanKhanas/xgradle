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
package org.altlinux.xgradle.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Parameters(separators = "=")
public class CliArgumentsContainer {

    @Parameter
    List<String> arguments = new ArrayList<>();

    @Parameter(
            names = "--xmvn-register",
            description = "Command to register an artifact",
            order = 1
    )
    private String xmvnRegister;

    @Parameter(
            names = "--install-gradle-plugin",
            description = "Install gradle plugin",
            order = 2
    )
    private boolean installPlugin;

    @Parameter(
            names = "--searching-directory",
            description = "Path to directory that contains artifacts",
            order = 3
    )
    private String searchingDirectory;

    @Parameter(
            names = "--artifact",
            description = "An artifact to process",
            order = 4
    )
    private String artifactName;

    @Parameter(
            names = "--pom-installation-dir",
            description = "Target directory to install pom files",
            order = 5
    )
    private String pomInstallationDirectory;

    @Parameter(
            names = "--jar-installation-dir",
            description = "Target directory to install jar files",
            order = 6
    )
    private String jarInstallationDirectory;

    @Parameter(
            names = "--help",
            help = true,
            description = "Display help information",
            order = 7
    )
    private boolean help;

    public boolean hasXmvnRegister() {
        return xmvnRegister != null && !xmvnRegister.isEmpty();
    }

    public String getXmvnRegister() {
        return xmvnRegister;
    }

    public String getSearchingDirectory() {
        return searchingDirectory;
    }

    public boolean hasSearchingDirectory() {
        return searchingDirectory != null && !searchingDirectory.isEmpty();
    }

    public Optional<String> getArtifactName() {
        return Optional.ofNullable(artifactName);
    }

    public boolean hasArtifactName() {
        return artifactName != null && !artifactName.isEmpty();
    }

    public boolean hasPomInstallationDirectory() {
        return pomInstallationDirectory != null && !pomInstallationDirectory.isEmpty();
    }

    public String getPomInstallationDirectory() {
        return pomInstallationDirectory;
    }

    public boolean hasJarInstallationDirectory() {
        return jarInstallationDirectory != null && !jarInstallationDirectory.isEmpty();
    }

    public String getJarInstallationDirectory() {
        return jarInstallationDirectory;
    }

    public boolean hasInstallPluginParameter() {
        return installPlugin;
    }

    public boolean hasHelp(){
        return help;
    }
}