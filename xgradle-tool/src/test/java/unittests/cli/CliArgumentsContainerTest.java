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
package unittests.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.altlinux.xgradle.impl.cli.CliArgumentsContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test arguments container")
public class CliArgumentsContainerTest {

    private static CliArgumentsContainer parse(String... argv) {
        CliArgumentsContainer args = new CliArgumentsContainer();
        JCommander.newBuilder()
                .addObject(args)
                .build()
                .parse(argv);
        return args;
    }

    private static CliArgumentsContainer parseAndValidate(String... argv) {
        CliArgumentsContainer args = parse(argv);
        args.validateMutuallyExclusive();
        return args;
    }

    @Test
    @DisplayName("Defaults: no args -> all optionals are empty, all flags are false, all has* are false")
    void defaultsNoArgs() {
        CliArgumentsContainer args = parse();

        assertAll(
                () -> assertFalse(args.hasXmvnRegister()),
                () -> assertFalse(args.hasSearchingDirectory()),
                () -> assertFalse(args.hasInstallPrefix()),
                () -> assertFalse(args.hasInstallPluginParameter()),
                () -> assertFalse(args.hasBomRegistration()),
                () -> assertFalse(args.hasJavadocRegistration()),
                () -> assertFalse(args.hasJarInstallationDirectory()),
                () -> assertFalse(args.hasPomInstallationDirectory()),
                () -> assertFalse(args.hasAllowSnapshots()),
                () -> assertFalse(args.hasRemoveParentPoms()),
                () -> assertFalse(args.hasVersion()),
                () -> assertFalse(args.hasHelp()),
                () -> assertFalse(args.hasArtifactName()),

                () -> assertFalse(args.isRecursive()),
                () -> assertFalse(args.hasAddDependencies()),
                () -> assertFalse(args.hasRemoveDependencies()),
                () -> assertFalse(args.hasChangeDependencies()),
                () -> assertFalse(args.hasPomRedaction()),

                () -> assertTrue(args.getArtifactName().isEmpty()),
                () -> assertNull(args.getExcludedArtifact()),
                () -> assertNull(args.getRemoveParentPoms()),
                () -> assertNull(args.getXmvnRegister()),
                () -> assertNull(args.getSearchingDirectory()),
                () -> assertNull(args.getInstallPrefix()),
                () -> assertNull(args.getJarInstallationDirectory()),
                () -> assertNull(args.getPomInstallationDirectory()),

                () -> assertNull(args.getAddDependencies()),
                () -> assertNull(args.getRemoveDependencies()),
                () -> assertNull(args.getChangeDependencies())
        );
    }

    @Test
    @DisplayName("JCommander: parses --xmvn-register and --searching-directory and reflects in has/get")
    void parsesXmvnRegisterCommand() {
        CliArgumentsContainer args = parseAndValidate(
                "--xmvn-register=/usr/bin/python3 /usr/share/java-utils/mvn_artifact.py",
                "--searching-directory=/home/xeno/.m2"
        );

        assertAll(
                () -> assertTrue(args.hasXmvnRegister()),
                () -> assertTrue(args.hasSearchingDirectory()),
                () -> assertEquals("/usr/bin/python3 /usr/share/java-utils/mvn_artifact.py", args.getXmvnRegister()),
                () -> assertEquals("/home/xeno/.m2", args.getSearchingDirectory())
        );
    }

    @Test
    @DisplayName("JCommander: parses --install-prefix and reflects in has/get")
    void parsesInstallPrefix() {
        CliArgumentsContainer args = parseAndValidate(
                "--install-prefix=/usr",
                "--searching-directory=/home/xeno/.m2",
                "--xmvn-register=/usr/bin/python3 /usr/share/java-utils/mvn_artifact.py"
        );

        assertAll(
                () -> assertTrue(args.hasInstallPrefix()),
                () -> assertEquals("/usr", args.getInstallPrefix())
        );
    }

    @Test
    @DisplayName("JCommander: parses --install-gradle-plugin and reflects in has*")
    void parsesInstallGradlePluginFlag() {
        CliArgumentsContainer args = parseAndValidate(
                "--install-gradle-plugin",
                "--searching-directory=/home/xeno/.m2",
                "--pom-installation-dir=/tmp/poms",
                "--jar-installation-dir=/tmp/jars"
        );

        assertAll(
                () -> assertTrue(args.hasInstallPluginParameter()),
                () -> assertTrue(args.hasSearchingDirectory()),
                () -> assertTrue(args.hasPomInstallationDirectory()),
                () -> assertTrue(args.hasJarInstallationDirectory())
        );
    }

    @Test
    @DisplayName("JCommander: parses --register-bom and reflects in has*")
    void parsesRegisterBomFlag() {
        CliArgumentsContainer args = parseAndValidate(
                "--register-bom",
                "--xmvn-register=/usr/bin/python3 /usr/share/java-utils/mvn_artifact.py",
                "--searching-directory=/home/xeno/.m2"
        );

        assertAll(
                () -> assertTrue(args.hasBomRegistration()),
                () -> assertTrue(args.hasXmvnRegister()),
                () -> assertTrue(args.hasSearchingDirectory())
        );
    }

    @Test
    @DisplayName("JCommander: parses --register-javadoc and reflects in has*")
    void parsesRegisterJavadocFlag() {
        CliArgumentsContainer args = parseAndValidate(
                "--register-javadoc",
                "--searching-directory=/home/xeno/.m2",
                "--jar-installation-dir=/tmp/javadoc-jars"
        );

        assertAll(
                () -> assertTrue(args.hasJavadocRegistration()),
                () -> assertTrue(args.hasSearchingDirectory()),
                () -> assertTrue(args.hasJarInstallationDirectory())
        );
    }

    @Test
    @DisplayName("JCommander: parses --artifacts list and exposes as Optional<List<String>>")
    void parsesArtifactsList() {
        CliArgumentsContainer args = parseAndValidate(
                "--searching-directory=/home/xeno/.m2",
                "--xmvn-register=/usr/bin/python3 /usr/share/java-utils/mvn_artifact.py",
                "--artifacts=aaa,bbb,ccc"
        );

        assertAll(
                () -> assertTrue(args.hasArtifactName()),
                () -> assertTrue(args.getArtifactName().isPresent()),
                () -> assertEquals(List.of("aaa", "bbb", "ccc"), args.getArtifactName().orElseThrow())
        );
    }

    @Test
    @DisplayName("JCommander: parses --exclude-artifacts list and reflects in getExcludedArtifact()")
    void parsesExcludeArtifactsList() {
        CliArgumentsContainer args = parseAndValidate(
                "--searching-directory=/home/xeno/.m2",
                "--xmvn-register=/usr/bin/python3 /usr/share/java-utils/mvn_artifact.py",
                "--exclude-artifacts=aaa,bbb"
        );

        assertEquals(List.of("aaa", "bbb"), args.getExcludedArtifact());
    }

    @Test
    @DisplayName("JCommander: parses --allow-snapshots flag")
    void parsesAllowSnapshotsFlag() {
        CliArgumentsContainer args = parseAndValidate(
                "--searching-directory=/home/xeno/.m2",
                "--xmvn-register=/usr/bin/python3 /usr/share/java-utils/mvn_artifact.py",
                "--allow-snapshots"
        );

        assertTrue(args.hasAllowSnapshots());
    }

    @Test
    @DisplayName("JCommander: parses --remove-parent list")
    void parsesRemoveParentList() {
        CliArgumentsContainer args = parseAndValidate(
                "--searching-directory=/home/xeno/.m2",
                "--xmvn-register=/usr/bin/python3 /usr/share/java-utils/mvn_artifact.py",
                "--remove-parent=aaa,bbb"
        );

        assertAll(
                () -> assertTrue(args.hasRemoveParentPoms()),
                () -> assertEquals(List.of("aaa", "bbb"), args.getRemoveParentPoms())
        );
    }

    @Test
    @DisplayName("JCommander: parses --version flag")
    void parsesVersionFlag() {
        CliArgumentsContainer args = parse("--version");
        assertTrue(args.hasVersion());
    }

    @Test
    @DisplayName("JCommander: parses --help flag")
    void parsesHelpFlag() {
        CliArgumentsContainer args = parse("--help");
        assertTrue(args.hasHelp());
    }

    @Test
    @DisplayName("Pom redaction: parses -r and --recursive")
    void parsesRecursiveFlag() {
        CliArgumentsContainer shortFlag = parseAndValidate(
                "-r",
                "--searching-directory=/home/xeno/.m2",
                "--remove-dependency=org.slf4j:slf4j-api"
        );

        CliArgumentsContainer longFlag = parseAndValidate(
                "--recursive",
                "--searching-directory=/home/xeno/.m2",
                "--remove-dependency=org.slf4j:slf4j-api"
        );

        assertAll(
                () -> assertTrue(shortFlag.isRecursive()),
                () -> assertTrue(longFlag.isRecursive())
        );
    }

    @Test
    @DisplayName("Pom redaction: parses --add-dependency list and enables hasPomRedaction")
    void parsesAddDependencyList() {
        CliArgumentsContainer args = parseAndValidate(
                "--searching-directory=/home/xeno/.m2",
                "--add-dependency=org.a:a:1.0:compile,org.b:b"
        );

        assertAll(
                () -> assertTrue(args.hasAddDependencies()),
                () -> assertEquals(List.of("org.a:a:1.0:compile", "org.b:b"), args.getAddDependencies()),
                () -> assertTrue(args.hasPomRedaction())
        );
    }

    @Test
    @DisplayName("Pom redaction: parses --remove-dependency list and enables hasPomRedaction")
    void parsesRemoveDependencyList() {
        CliArgumentsContainer args = parseAndValidate(
                "--searching-directory=/home/xeno/.m2",
                "--remove-dependency=org.a:a:1.0:compile,org.b:b"
        );

        assertAll(
                () -> assertTrue(args.hasRemoveDependencies()),
                () -> assertEquals(List.of("org.a:a:1.0:compile", "org.b:b"), args.getRemoveDependencies()),
                () -> assertTrue(args.hasPomRedaction())
        );
    }

    @Test
    @DisplayName("Pom redaction: parses --change-dependency exactly 2 values")
    void parsesChangeDependencyPair() {
        CliArgumentsContainer args = parseAndValidate(
                "--searching-directory=/home/xeno/.m2",
                "--change-dependency=org.a:a:1.0:compile,org.a:a:2.0:test"
        );

        assertAll(
                () -> assertTrue(args.hasChangeDependencies()),
                () -> assertEquals(List.of("org.a:a:1.0:compile", "org.a:a:2.0:test"), args.getChangeDependencies()),
                () -> assertTrue(args.hasPomRedaction())
        );
    }

    @Test
    @DisplayName("validateMutuallyExclusive: rejects --change-dependency with not exactly 2 values")
    void rejectsChangeDependencyWithWrongArity() {
        ParameterException ex = assertThrows(
                ParameterException.class,
                () -> parseAndValidate(
                        "--searching-directory=/home/xeno/.m2",
                        "--change-dependency=org.a:a:1.0:compile"
                )
        );
        assertTrue(ex.getMessage().contains("--change-dependency requires exactly 2 values"));
    }

    @Test
    @DisplayName("validateMutuallyExclusive: rejects mixing dependency operations")
    void rejectsMixingDependencyOperations() {
        ParameterException ex = assertThrows(
                ParameterException.class,
                () -> parseAndValidate(
                        "--searching-directory=/home/xeno/.m2",
                        "--add-dependency=org.a:a",
                        "--remove-dependency=org.b:b"
                )
        );
        assertTrue(ex.getMessage().contains("Conflicting parameters"));
    }

    @Test
    @DisplayName("validateMutuallyExclusive: pom redaction requires --searching-directory")
    void pomRedactionRequiresSearchingDirectory() {
        ParameterException ex = assertThrows(
                ParameterException.class,
                () -> parseAndValidate("--remove-dependency=org.a:a")
        );
        assertTrue(ex.getMessage().contains("No searching directory specified"));
    }

    @Test
    @DisplayName("validateMutuallyExclusive: rejects pom redaction mixed with other modes")
    void rejectsPomRedactionWithOtherModes() {
        ParameterException ex = assertThrows(
                ParameterException.class,
                () -> parseAndValidate(
                        "--searching-directory=/home/xeno/.m2",
                        "--remove-dependency=org.a:a",
                        "--xmvn-register=/usr/bin/python3 /usr/share/java-utils/mvn_artifact.py"
                )
        );
        assertTrue(ex.getMessage().contains("Conflicting parameters"));
    }

    @Test
    @DisplayName("validateMutuallyExclusive: rejects --xmvn-register and --install-gradle-plugin")
    void rejectsXmvnRegisterAndInstallGradlePlugin() {
        ParameterException ex = assertThrows(
                ParameterException.class,
                () -> parseAndValidate(
                        "--xmvn-register=/usr/bin/python3 /usr/share/java-utils/mvn_artifact.py",
                        "--install-gradle-plugin",
                        "--searching-directory=/home/xeno/.m2",
                        "--pom-installation-dir=/tmp/poms",
                        "--jar-installation-dir=/tmp/jars"
                )
        );
        assertTrue(ex.getMessage().contains("Conflicting parameters"));
    }

    @Test
    @DisplayName("validateMutuallyExclusive: rejects --install-gradle-plugin and --register-bom")
    void rejectsInstallGradlePluginAndRegisterBom() {
        ParameterException ex = assertThrows(
                ParameterException.class,
                () -> parseAndValidate(
                        "--install-gradle-plugin",
                        "--register-bom",
                        "--searching-directory=/home/xeno/.m2",
                        "--pom-installation-dir=/tmp/poms",
                        "--jar-installation-dir=/tmp/jars"
                )
        );
        assertTrue(ex.getMessage().contains("Conflicting parameters"));
    }

    @Test
    @DisplayName("validateMutuallyExclusive: rejects --register-javadoc and --register-bom")
    void rejectsRegisterJavadocAndRegisterBom() {
        ParameterException ex = assertThrows(
                ParameterException.class,
                () -> parseAndValidate(
                        "--register-javadoc",
                        "--register-bom",
                        "--searching-directory=/home/xeno/.m2",
                        "--xmvn-register=/usr/bin/python3 /usr/share/java-utils/mvn_artifact.py",
                        "--jar-installation-dir=/tmp/javadoc-jars"
                )
        );
        assertTrue(ex.getMessage().contains("Conflicting parameters"));
    }

    @Test
    @DisplayName("validateMutuallyExclusive: rejects --register-javadoc and --xmvn-register")
    void rejectsRegisterJavadocAndXmvnRegister() {
        ParameterException ex = assertThrows(
                ParameterException.class,
                () -> parseAndValidate(
                        "--register-javadoc",
                        "--xmvn-register=/usr/bin/python3 /usr/share/java-utils/mvn_artifact.py",
                        "--searching-directory=/home/xeno/.m2",
                        "--jar-installation-dir=/tmp/javadoc-jars"
                )
        );
        assertTrue(ex.getMessage().contains("Conflicting parameters"));
    }

    @Test
    @DisplayName("validateMutuallyExclusive: rejects --register-javadoc and --install-gradle-plugin")
    void rejectsRegisterJavadocAndInstallGradlePlugin() {
        ParameterException ex = assertThrows(
                ParameterException.class,
                () -> parseAndValidate(
                        "--register-javadoc",
                        "--install-gradle-plugin",
                        "--searching-directory=/home/xeno/.m2",
                        "--pom-installation-dir=/tmp/poms",
                        "--jar-installation-dir=/tmp/jars"
                )
        );
        assertTrue(ex.getMessage().contains("Conflicting parameters"));
    }
}
