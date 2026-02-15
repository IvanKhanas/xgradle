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

package org.altlinux.xgradle.interfaces.handlers;

import org.gradle.api.invocation.Gradle;

/**
 * Handler for configuring project dependencies after Gradle configuration phase.
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface ProjectDependenciesHandler extends Handler<Gradle> {

    /**
     * Performs full dependency-resolution pipeline after project configuration:
     *  - collects project dependencies
     *  - processes BOMs
     *  - resolves system artifacts
     *  - processes transitive dependencies
     *  - applies dependency substitutions
     *  - configures artifacts into configurations
     *
     * @param gradle current Gradle instance
     */
    @Override
    void handle(Gradle gradle);
}
