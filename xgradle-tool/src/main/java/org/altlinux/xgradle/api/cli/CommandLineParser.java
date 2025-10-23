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
package org.altlinux.xgradle.api.cli;

import java.util.List;

/**
 * Interface for command-line parsing operations.
 * Defines contract for parsing command strings into executable parts.
 *
 * @author Ivan Khanas
 */
public interface CommandLineParser {

    /**
     * Parses a command string into executable parts.
     *
     * @param command the command string to parse
     * @return list of command parts
     */
    List<String> parseCommandLine(String command);
}
