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

import com.beust.jcommander.IUsageFormatter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;

import java.util.Comparator;
import java.util.List;

/**
 * Custom usage formatter for JCommander command-line parsing.
 * Implements {@link IUsageFormatter}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public final class CustomXgradleFormatter implements IUsageFormatter {
    private final JCommander commander;

    public CustomXgradleFormatter(JCommander commander) {
        this.commander = commander;
    }

    @Override
    public void usage(StringBuilder out) {
        usage(out, "");
    }

    @Override
    public void usage(StringBuilder out, String indent) {
        out.append("Usage examples: \n\n")
                .append("Registration: \n")
                .append("xgradle-tool --xmvn-register=\"<registration command>\" --searching-directory=<directory path> " +
                        "| (optional) --artifacts=<artifactName> | (optional) --exclude-artifacts=<artifactName>\n\n")
                .append("BOM registration: \n")
                .append("xgradle-tool --xmvn-register=\"<registration command>\" --register-bom --searching-directory=<directory path> " +
                        "| (optional) --artifacts=<artifactName> | (optional) --exclude-artifacts=<artifactName>\n\n")
                .append("Javadoc registration: \n")
                .append("xgradle-tool --register-javadoc --searching-directory=<directory path> " +
                        "--jar-installation-dir=<directory path> " +
                        "[--install-prefix=<prefix>] " +
                        "| (optional) --artifacts=<artifactName> | (optional) --exclude-artifacts=<artifactName>\n\n")
                .append("Plugins installation: \n")
                .append("xgradle-tool --install-gradle-plugin --artifacts=<artifactName> " +
                        "--searching-directory=<directory path> " +
                        "--pom-installation-dir=</path/to/poms/installation/location> " +
                        "--jar-installation-dir=</path/to/jars/installation/location>\n\n")
                .append("-------------------------------\n\n")
                .append("Dependency redaction (.pom files): \n")
                .append("You can add -r or --recursive to process all .pom files under --searching-directory recursively.\n\n")
                .append("Remove dependency: \n")
                .append("xgradle-tool --searching-directory=</path/to/file.pom|/path/to/dir> --remove-dependency=<groupId:artifactId[:version[:scope]]>\n\n")
                .append("Add dependency: \n")
                .append("xgradle-tool --searching-directory=</path/to/file.pom|/path/to/dir> --add-dependency=<groupId:artifactId[:version[:scope]]>\n\n")
                .append("Change dependency: \n")
                .append("xgradle-tool --searching-directory=</path/to/file.pom|/path/to/dir> --change-dependency <sourceCoords> <targetCoords>\n\n")
                .append("Usage: ").append(commander.getProgramName()).append(" [options]\n\n")
                .append("Options:\n");

        List<ParameterDescription> parameters = commander.getParameters();
        parameters.sort(Comparator.comparingInt(p -> p.getParameter().order()));

        for (ParameterDescription param : parameters) {
            out.append(indent)
                    .append(String.format("  %-25s", param.getNames()))
                    .append(param.getDescription())
                    .append("\n\n");
        }
    }


    @Override
    public void usage(String commandName) {}

    @Override
    public void usage(String commandName, StringBuilder out) {
        usage(out);
    }

    @Override
    public void usage(String commandName, StringBuilder out, String indent) {
        usage(out, indent);
    }

    @Override
    public String getCommandDescription(String commandName) {
        return "";
    }
}
