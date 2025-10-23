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
package org.altlinux.xgradle.cli.commands;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Command-line utility for displaying application version information.
 * Reads version details from application.properties file.
 * Formats and displays version information in a banner format.
 */
public class CliVersion {

    /**
     * Prints application version information including version, commit hash, and build time.
     * Displays information in a formatted banner.
     *
     * @throws FileNotFoundException if application.properties file is not found
     */
    public final void printVersion() throws FileNotFoundException {
        Properties prop = new Properties();
        InputStream readBuildInfo = getClass().getResourceAsStream("/application.properties");

        try {
            prop.load(readBuildInfo);
        } catch (IOException e) {
            throw new FileNotFoundException("Could not find /application.properties");
        }

        String VERSION = prop.getProperty("version");
        String COMMITHASH = prop.getProperty("commitHash");
        String BUILDTIME = prop.getProperty("buildTime", "N/A");

        String BANNER_TEMPLATE = "----------------------------------------------------------\n" +
                "| Version: %-45s |\n" +
                "| Revision: %-44s |\n" +
                "| Build Time: %-42s |\n" +
                "----------------------------------------------------------";
        System.out.printf(BANNER_TEMPLATE + "%n", VERSION, COMMITHASH, BUILDTIME);
    }
}