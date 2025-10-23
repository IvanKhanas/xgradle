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
package org.altlinux.xgradle;

/**
 * Application exit codes enumeration.
 * Defines standard exit codes for successful and error termination.
 * Provides utility methods for exiting with specific codes.
 */
public enum ExitCode {
    SUCCESS(0),
    ERROR(1);

    private final int exitCode;

    ExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    /**
     * Terminates the application with the specified exit code.
     */
    public void exit() {
        System.exit(this.exitCode);
    }

    /**
     * Gets the exit code value.
     *
     * @return the exit code integer value
     */
    public int getExitCode() {
        return exitCode;
    }
}
