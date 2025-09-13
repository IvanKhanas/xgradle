package org.altlinux.xgradle.cli;

import org.altlinux.xgradle.api.cli.CommandLineParser;

import java.util.List;
import java.util.ArrayList;

public class DefaultCommandLineParser implements CommandLineParser {

    @Override
    public List<String> parseCommandLine(String command) {
        List<String> parts = new ArrayList<>();

        StringBuilder builder = new StringBuilder();
        boolean inQuotes = false;
        char quote = '"';

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            if (c=='"' || c=='\'') {
                if (inQuotes) {
                    if (c == quote) {
                        inQuotes = false;

                        if (builder.length()>0) {
                            parts.add(builder.toString());
                            builder = new StringBuilder();
                        }
                    } else {builder.append(c);}
                } else {inQuotes = true; quote = c;}
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (builder.length()>0) {
                    parts.add(builder.toString());
                    builder = new StringBuilder();
                }
            } else {builder.append(c);}
        }

        if (builder.length()>0) {
            parts.add(builder.toString());
        }
        return parts;
    }
}