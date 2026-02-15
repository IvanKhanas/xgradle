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
package org.altlinux.xgradle.impl.utils.ui;

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
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public class LogoPrinter {

    private static final String ART_FILE = "logo.txt";

    public static boolean isLogoEnabled() {
        return !"true".equals(System.getProperty("disable.logo"));
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
            System.out.println("\n");
        } catch (Exception e) {
            throw new RuntimeException("Banner printing error",e);
        }
    }

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
