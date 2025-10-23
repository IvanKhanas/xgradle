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
package org.altlinux.xgradle.utils;

/**
 * Class for coloring plugin logs.
 *
 * @author Ivan Khanas
 */
public class Painter {

    private static final boolean COLOR_ENABLED = isColorEnabled();

    public static final String RESET = "\u001B[0m";
    public static final String GREEN = "\u001B[32m";
    public static final String CYAN = "\u001B[36m";
    public static final String YELLOW = "\u001B[33m";

    /**
     * Checks if the coloring option is enabled when building the plugin.
     *
     * @return {@code true} if enabled and {@code false} otherwise
     */
    private static boolean isColorEnabled() {
        return "true".equals(System.getProperty("enable.ansi.color"));
    }

    /**
     * Colors the string green.
     *
     * @param text text to color
     *
     * @return Row with inserted ANSI blocks of green color
     */
    public static String green(String text) {
        return colorize(text, GREEN);
    }

    /**
     * Colors the string cyan.
     *
     * @param text text to color
     *
     * @return Row with inserted ANSI blocks of cyan color
     */
    public static String cyan(String text) {
        return colorize(text, CYAN);
    }

    /**
     * Colors the string yellow.
     *
     * @param text text to color.
     *
     * @return Row with inserted ANSI blocks of yellow color.
     */
    public static String yellow(String text) {
        return colorize(text, YELLOW);
    }

    /**
     * Colors the line in the selected color.
     *
     * @param text text to color
     * @param ansiColor ANSI block
     *
     * @return string with ANSI blocks
     */
    private static String colorize(String text, String ansiColor) {
        return COLOR_ENABLED ? (ansiColor + text + RESET) : text;
    }
}