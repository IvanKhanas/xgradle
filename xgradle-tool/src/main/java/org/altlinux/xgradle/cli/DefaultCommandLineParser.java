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

import org.altlinux.xgradle.api.cli.CommandLineParser;

import java.util.ArrayList;
import java.util.List;

public class DefaultCommandLineParser implements CommandLineParser {

    @Override
    public List<String> parseCommandLine(String command) {
        List<String> parts = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            if ((c == '"' || c == '\'') && !inQuotes) {
                inQuotes = true;
                quoteChar = c;
            } else if (inQuotes && c == quoteChar) {
                inQuotes = false;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (builder.length() > 0) {
                    parts.addAll(splitQuotedArg(builder.toString()));
                    builder.setLength(0);
                }
            } else {
                builder.append(c);
            }
        }

        if (builder.length() > 0) {
            parts.addAll(splitQuotedArg(builder.toString()));
        }

        return parts;
    }

    private List<String> splitQuotedArg(String arg) {
        List<String> result = new ArrayList<>();
        String trimmed = arg.trim();

        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
                (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }

        for (String s : trimmed.split("\\s+")) {
            if (!s.isEmpty()) {
                result.add(s);
            }
        }
        return result;
    }
}
