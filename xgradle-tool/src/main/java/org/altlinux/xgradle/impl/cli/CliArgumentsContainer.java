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
package org.altlinux.xgradle.impl.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Container for command-line arguments using JCommander annotations.
 * Defines and manages all supported command-line parameters.
 *
 * @author IvanKhanas
 */
@Parameters(separators = "=")
public class CliArgumentsContainer {

    @Parameter(
            names = "--xmvn-register",
            description = "Command to register an artifacts for xmvn",
            order = 1
    )
    private String xmvnRegister;

    @Parameter(
            names = "--register-bom",
            description = "Register BOM files for xmvn",
            order = 2
    )
    private boolean registerBom;

    @Parameter(
            names = "--register-javadoc",
            description = "Register javadoc JAR files for xmvn",
            order = 3
    )
    private boolean registerJavadoc;

    @Parameter(
            names = "--install-prefix",
            description = "Prefix to strip from installation paths when writing to .mfiles-javadoc",
            order = 4
    )
    private String installPrefix;

    @Parameter(
            names = "--install-gradle-plugin",
            description = "Install gradle plugin",
            order = 5
    )
    private boolean installPlugin;

    @Parameter(
            names = "--searching-directory",
            description = "Path to directory that contains artifacts",
            order = 6
    )
    private String searchingDirectory;

    @Parameter(
            names = "--artifacts",
            description = "Processes all artifacts whose names begin with the passed value (comma-separated listing)",
            order = 7
    )
    private List<String> artifactNames;

    @Parameter(
            names = "--pom-installation-dir",
            description = "Target directory to install POM files (required for gradle plugins installation)",
            order = 8
    )
    private String pomInstallationDirectory;

    @Parameter(
            names = "--jar-installation-dir",
            description = "Target directory to install JAR files (required for gradle plugins installation)",
            order = 9
    )
    private String jarInstallationDirectory;

    @Parameter(
            names = "--exclude-artifacts",
            description = "Excludes all artifacts whose names begin with the passed value (comma-separated listing)",
            order = 10
    )
    private List<String> excludedArtifacts;

    @Parameter(
            names = "--allow-snapshots",
            description = "Allows processing of snapshot artifacts",
            order = 11
    )
    private boolean allowSnapshots;

    @Parameter(
            names = "--remove-parent",
            description = "Remove parent block from POM file",
            order = 12
    )
    private List<String> removeParentPoms;

    @Parameter(
            names = "--version",
            description = "Show cli version",
            order = 13
    )
    private boolean version;

    @Parameter(
            names = "--help",
            help = true,
            description = "Display help information",
            order = 14
    )
    private boolean help;

    public void validateMutuallyExclusive() {
        List<String> conflictingParams = new ArrayList<>();

        if (hasXmvnRegister() && hasInstallPluginParameter()) {
            conflictingParams.add("--xmvn-register and --install-gradle-plugin");
        }

        if (hasInstallPluginParameter() && hasBomRegistration()) {
            conflictingParams.add("--install-gradle-plugin and --register-bom");
        }

        if(hasJavadocRegistration() && hasInstallPluginParameter()) {
            conflictingParams.add("--register-javadoc and --install-gradle-plugin");
        }

        if(hasJavadocRegistration() && hasBomRegistration()) {
            conflictingParams.add("--register-javadoc and --register-bom");
        }

        if(hasJavadocRegistration() && hasXmvnRegister()) {
            conflictingParams.add("--register-javadoc and --xmvn-register");
        }

        if (!conflictingParams.isEmpty()) {
            throw new ParameterException(
                    "Conflicting parameters: " + String.join(", ", conflictingParams) +
                            "\nUse only one main mode at a time."
            );
        }
    }

    /**
     * Checks if XMvn register command is specified.
     *
     * @return true if XMvn register command is specified, false otherwise
     */
    public boolean hasXmvnRegister() {
        return xmvnRegister != null && !xmvnRegister.isEmpty();
    }

    /**
     * Gets the XMvn register command.
     *
     * @return the XMvn register command string
     */
    public String getXmvnRegister() {
        return xmvnRegister;
    }

    /**
     * Gets the searching directory path.
     *
     * @return the searching directory path
     */
    public String getSearchingDirectory() {
        return searchingDirectory;
    }

    /**
     * Checks if searching directory is specified.
     *
     * @return true if searching directory is specified, false otherwise
     */
    public boolean hasSearchingDirectory() {
        return searchingDirectory != null && !searchingDirectory.isEmpty();
    }

    /**
     * Gets the optional list of artifact names.
     *
     * @return optional containing list of artifact names, or empty if not specified
     */
    public Optional<List<String>> getArtifactName() {
        return Optional.ofNullable(artifactNames);
    }

    /**
     * Checks if artifact names are specified.
     *
     * @return true if artifact names are specified, false otherwise
     */
    public boolean hasArtifactName() {
        return artifactNames != null && !artifactNames.isEmpty();
    }

    /**
     * Checks if POM installation directory is specified.
     *
     * @return true if POM installation directory is specified, false otherwise
     */
    public boolean hasPomInstallationDirectory() {
        return pomInstallationDirectory != null && !pomInstallationDirectory.isEmpty();
    }

    /**
     * Gets the POM installation directory path.
     *
     * @return the POM installation directory path
     */
    public String getPomInstallationDirectory() {
        return pomInstallationDirectory;
    }

    /**
     * Checks if JAR installation directory is specified.
     *
     * @return true if JAR installation directory is specified, false otherwise
     */
    public boolean hasJarInstallationDirectory() {
        return jarInstallationDirectory != null && !jarInstallationDirectory.isEmpty();
    }

    /**
     * Gets the JAR installation directory path.
     *
     * @return the JAR installation directory path
     */
    public String getJarInstallationDirectory() {
        return jarInstallationDirectory;
    }

    /**
     * Checks if install plugin parameter is specified.
     *
     * @return true if install plugin parameter is specified, false otherwise
     */
    public boolean hasInstallPluginParameter() {
        return installPlugin;
    }

    /**
     * Checks if BOM installation is specified.
     *
     * @return true if BOM installation is specified, false otherwise
     */
    public boolean hasBomRegistration(){
        return registerBom;
    }

    /**
     * Gets the list of excluded artifacts.
     *
     * @return list of excluded artifact patterns
     */
    public List<String> getExcludedArtifact() {
        return excludedArtifacts;
    }

    /**
     * Checks if snapshot artifacts are allowed.
     *
     * @return true if snapshot artifacts are allowed, false otherwise
     */
    public boolean hasAllowSnapshots() {
        return allowSnapshots;
    }

    /**
     * Checks if help is requested.
     *
     * @return true if help is requested, false otherwise
     */
    public boolean hasHelp(){
        return help;
    }

    /**
     * Gets the list of POM files for which to remove parent blocks.
     *
     * @return list of POM patterns for parent removal
     */
    public List<String> getRemoveParentPoms(){
        return removeParentPoms;
    }

    /**
     * Checks if parent removal is specified.
     *
     * @return true if parent removal is specified, false otherwise
     */
    public boolean hasRemoveParentPoms() {
        return removeParentPoms != null && !removeParentPoms.isEmpty();
    }

    /**
     * Checks if Javadoc registration is specified.
     *
     * @return true if Javadoc registration is specified, false otherwise
     */
    public boolean hasJavadocRegistration() {
        return registerJavadoc;
    }

    /**
     * Checks if install prefix is specified.
     * @return true if install prefix is specified and not empty
     */
    public boolean hasInstallPrefix() {
        return installPrefix != null && !installPrefix.isEmpty();
    }

    /**
     * Gets the install prefix value.
     * @return install prefix string or null if not specified
     */
    public String getInstallPrefix() {
        return installPrefix;
    }

    /**
     * Checks if version information is requested.
     *
     * @return true if version information is requested, false otherwise
     */
    public boolean hasVersion() {
        return version;
    }
}
