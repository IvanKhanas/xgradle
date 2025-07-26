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
package org.altlinux.gradlePlugin.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for printing logo when plugin is activated.
 *
 * @author Ivan Khanas
 */
public class LogoPrinter {

    private static final String ART_FILE = "logo.txt";

    /**
     * Checks if the logo printing option is enabled when building the plugin.
     *
     * @return {@code true} if enabled and {@code false} otherwise
     */
    public static boolean isLogoEnabled(){
        if("true".equals(System.getProperty("disable.logo"))) {
            return false;
        }
        return true;
    }

    public static void printCenteredBanner() {
        try {
            List<String> artLines = loadArtFromResources();
            int terminalWidth = getTerminalWidth();

            int maxLength = 0;
            for (String line : artLines) {
                if (line.length() > maxLength) {
                    maxLength = line.length();
                }
            }

            int padding = Math.max(0, (terminalWidth - maxLength) / 2);

            for (String line : artLines) {
                System.out.println(" ".repeat(padding) + line);
            }
        } catch (Exception e) {
            throw new RuntimeException("Banner printing error",e);
        }
    }

    /**
     * Loads ASCII art lines from the classpath resource.
     * <p>
     * @return list of strings representing each line of the logo
     *
     * @throws IOException if an I/O error occurs while reading the resource
     * @throws AssertionError if the resource stream is null (resource not found)
     */
    private static List<String> loadArtFromResources() throws IOException {
        List<String> lines = new ArrayList<>();
        try (InputStream is = LogoPrinter.class.getClassLoader().getResourceAsStream(ART_FILE)) {
            assert is != null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    /**
     * Attempts to determine the current terminal width.
     * <p>
     * On Linux systems, executes the {@code tput cols} command to get the width.
     * On other systems or if an error occurs, returns a default width of 80 columns.
     *
     * @return terminal width in characters, or 80 if it cannot be determined
     */
    private static int getTerminalWidth() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("linux")) {
                Process process = Runtime.getRuntime()
                        .exec(new String[]{"bash", "-c", "tput cols"});

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {

                    String width = reader.readLine();
                    if (width != null) {
                        return Integer.parseInt(width.trim());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return 80;
    }
}