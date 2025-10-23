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

import org.altlinux.xgradle.api.cli.CommandExecutor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Default implementation of CommandExecutor.
 * Handles execution of system commands with output streaming.
 *
 * @author Ivan Khanas
 */
@Singleton
public class DefaultCommandExecutor implements CommandExecutor {

    /**
     * Executes the specified process builder and returns the exit code.
     * Streams command output to system out.
     *
     * @param processBuilder the process builder to execute
     * @return the exit code of the executed process
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the process is interrupted
     */
    @Override
    public int execute(ProcessBuilder processBuilder) throws IOException, InterruptedException {
        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(System.out::println);
        }

        return process.waitFor();
    }
}