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

import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.cli.CommandLineParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link CommandLineParser}.
 * Implements {@link CommandLineParser}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultCommandLineParser implements CommandLineParser {

    @Override
    public List<String> parseCommandLine(String command) {
        if (command == null || command.isBlank()) {
            return List.of();
        }

        String input = stripWrappingQuotes(command.trim());

        List<String> parts = new ArrayList<>();
        StringBuilder token = new StringBuilder();

        boolean inQuotes = false;
        char quoteChar = 0;
        boolean escaping = false;

        for (int characterIndex = 0; characterIndex < input.length(); characterIndex++) {
            char currentCharacter = input.charAt(characterIndex);

            if (escaping) {
                token.append(currentCharacter);
                escaping = false;
                continue;
            }

            if (inQuotes && currentCharacter == '\\') {
                escaping = true;
                continue;
            }

            if (!inQuotes && (currentCharacter == '"' || currentCharacter == '\'')) {
                inQuotes = true;
                quoteChar = currentCharacter;
                continue;
            }

            if (inQuotes && currentCharacter == quoteChar) {
                inQuotes = false;
                quoteChar = 0;
                continue;
            }

            if (!inQuotes && Character.isWhitespace(currentCharacter)) {
                flush(parts, token);
                continue;
            }

            token.append(currentCharacter);
        }

        if (inQuotes) {
            throw new IllegalArgumentException("Unclosed quote in command: " + command);
        }

        flush(parts, token);
        return parts;
    }

    private static void flush(List<String> parts, StringBuilder token) {
        if (token.length() == 0) {
            return;
        }
        parts.add(token.toString());
        token.setLength(0);
    }

    private static String stripWrappingQuotes(String value) {
        if (value.length() < 2) {
            return value;
        }
        char firstCharacter = value.charAt(0);
        char lastCharacter = value.charAt(value.length() - 1);

        if ((firstCharacter == '"' && lastCharacter == '"')
                || (firstCharacter == '\'' && lastCharacter == '\'')) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
