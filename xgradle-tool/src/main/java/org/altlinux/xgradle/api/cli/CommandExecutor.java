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

import java.io.IOException;

/**
 * Interface for command execution operations.
 * Defines contract for executing system commands.
 *
 * @author Ivan Khanas
 */
public interface CommandExecutor {

    /**
     * Executes the specified process builder and returns the exit code.
     *
     * @param processBuilder the process builder to execute
     * @return the exit code of the executed process
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the process is interrupted
     */
    int execute(ProcessBuilder processBuilder) throws IOException, InterruptedException;
}
