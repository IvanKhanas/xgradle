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
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Container for command-line arguments using JCommander annotations.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Parameters(separators = "=")
@Singleton
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

    @Parameter(
            names = {"-r", "--recursive"},
            description = "Recursively process all .pom files under --searching-directory",
            order = 15
    )
    private boolean recursive;

    @Parameter(
            names = "--add-dependency",
            description = "Add dependency: groupId:artifactId[:version[:scope]]",
            order = 16
    )
    private List<String> addDependencies;

    @Parameter(
            names = "--remove-dependency",
            description = "Remove dependency: groupId:artifactId[:version[:scope]]",
            order = 17
    )
    private List<String> removeDependencies;

    @Parameter(
            names = "--change-dependency",
            description = "Change dependency: pass exactly two values (source then target), each is groupId:artifactId[:version[:scope]]",
            order = 18
    )
    private List<String> changeDependencies;

    public void validateMutuallyExclusive() {
        List<String> conflictingParams = new ArrayList<>();

        if (hasXmvnRegister() && hasInstallPluginParameter()) {
            conflictingParams.add("--xmvn-register and --install-gradle-plugin");
        }

        if (hasInstallPluginParameter() && hasBomRegistration()) {
            conflictingParams.add("--install-gradle-plugin and --register-bom");
        }

        if (hasJavadocRegistration() && hasInstallPluginParameter()) {
            conflictingParams.add("--register-javadoc and --install-gradle-plugin");
        }

        if (hasJavadocRegistration() && hasBomRegistration()) {
            conflictingParams.add("--register-javadoc and --register-bom");
        }

        if (hasJavadocRegistration() && hasXmvnRegister()) {
            conflictingParams.add("--register-javadoc and --xmvn-register");
        }

        int depOps = (hasAddDependencies() ? 1 : 0)
                + (hasRemoveDependencies() ? 1 : 0)
                + (hasChangeDependencies() ? 1 : 0);

        if (depOps > 1) {
            conflictingParams.add("--add-dependency, --remove-dependency and --change-dependency");
        }

        if (hasChangeDependencies() && getChangeDependencies().size() != 2) {
            throw new ParameterException("--change-dependency requires exactly 2 values: <source> <target>");
        }

        if (hasPomRedaction()) {
            if (!hasSearchingDirectory()) {
                throw new ParameterException("No searching directory specified");
            }

            if (hasXmvnRegister()) {
                conflictingParams.add("--pom-redaction and --xmvn-register");
            }

            if (hasInstallPluginParameter()) {
                conflictingParams.add("--pom-redaction and --install-gradle-plugin");
            }

            if (hasBomRegistration()) {
                conflictingParams.add("--pom-redaction and --register-bom");
            }

            if (hasJavadocRegistration()) {
                conflictingParams.add("--pom-redaction and --register-javadoc");
            }
        }

        if (!conflictingParams.isEmpty()) {
            throw new ParameterException(
                    "Conflicting parameters: " + String.join(", ", conflictingParams) +
                            "\nUse only one main mode at a time."
            );
        }
    }

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

    public Optional<List<String>> getArtifactName() {
        return Optional.ofNullable(artifactNames);
    }

    public boolean hasArtifactName() {
        return artifactNames != null && !artifactNames.isEmpty();
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

    public boolean hasBomRegistration(){
        return registerBom;
    }

    public List<String> getExcludedArtifact() {
        return excludedArtifacts;
    }

    public boolean hasAllowSnapshots() {
        return allowSnapshots;
    }

    public boolean hasHelp(){
        return help;
    }

    public List<String> getRemoveParentPoms(){
        return removeParentPoms;
    }

    public boolean hasRemoveParentPoms() {
        return removeParentPoms != null && !removeParentPoms.isEmpty();
    }

    public boolean hasJavadocRegistration() {
        return registerJavadoc;
    }

    public boolean hasInstallPrefix() {
        return installPrefix != null && !installPrefix.isEmpty();
    }

    public String getInstallPrefix() {
        return installPrefix;
    }

    public boolean hasVersion() {
        return version;
    }


    public boolean isRecursive() {
        return recursive;
    }

    public boolean hasAddDependencies() {
        return addDependencies != null && !addDependencies.isEmpty();
    }

    public List<String> getAddDependencies() {
        return addDependencies;
    }

    public boolean hasRemoveDependencies() {
        return removeDependencies != null && !removeDependencies.isEmpty();
    }

    public List<String> getRemoveDependencies() {
        return removeDependencies;
    }

    public boolean hasChangeDependencies() {
        return changeDependencies != null && !changeDependencies.isEmpty();
    }

    public List<String> getChangeDependencies() {
        return changeDependencies;
    }

    public boolean hasPomRedaction() {
        return hasAddDependencies() || hasRemoveDependencies() || hasChangeDependencies();
    }

}
