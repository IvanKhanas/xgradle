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

import com.google.inject.Singleton;

import com.beust.jcommander.IUsageFormatter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;

import java.util.Comparator;
import java.util.List;

/**
 * Custom usage formatter for JCommander command-line parsing.
 * Provides formatted help output with usage examples and parameter descriptions.
 *
 * @author Ivan Khanas
 */
@Singleton
public class CustomXgradleFormatter implements IUsageFormatter {
    private final JCommander commander;

    /**
     * Constructs a new CustomXgradleFormatter with the specified JCommander instance.
     *
     * @param commander the JCommander instance to format usage for
     */
    public CustomXgradleFormatter(JCommander commander) {
        this.commander = commander;
    }

    /**
     * Generates usage information and appends it to the specified StringBuilder.
     *
     * @param out the StringBuilder to append usage information to
     */
    @Override
    public void usage(StringBuilder out) {
        usage(out, "");
    }

    /**
     * Generates usage information with indentation and appends it to the specified StringBuilder.
     *
     * @param out the StringBuilder to append usage information to
     * @param indent the indentation string to use
     */
    @Override
    public void usage(StringBuilder out, String indent) {
        out.append("Usage examples: \n\n")
                .append("Registration: \n")
                .append("xgradle-tool --xmvn-register=\"<registration command>\" --searching-directory=<directory path> " +
                        "| (optional) --artifacts=<artifactName> | (optional) --exclude-artifacts=<artifactName>\n\n")
                .append("BOM registration: \n")
                .append("xgradle-tool --xmvn-register=\"<registration command>\" --register-bom --searching-directory=<directory path> " +
                        "| (optional) --artifacts=<artifactName> | (optional) --exclude-=<artifactName>\n\n")
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

    /**
     * Generates usage information for a specific command.
     *
     * @param commandName the name of the command
     */
    @Override
    public void usage(String commandName) {}

    /**
     * Generates usage information for a specific command and appends it to the specified StringBuilder.
     *
     * @param commandName the name of the command
     * @param out the StringBuilder to append usage information to
     */
    @Override
    public void usage(String commandName, StringBuilder out) {
        usage(out);
    }

    /**
     * Generates usage information for a specific command with indentation and appends it to the specified StringBuilder.
     *
     * @param commandName the name of the command
     * @param out the StringBuilder to append usage information to
     * @param indent the indentation string to use
     */
    @Override
    public void usage(String commandName, StringBuilder out, String indent) {
        usage(out, indent);
    }

    /**
     * Gets the description for a specific command.
     *
     * @param commandName the name of the command
     * @return the command description
     */
    @Override
    public String getCommandDescription(String commandName) {
        return "";
    }
}