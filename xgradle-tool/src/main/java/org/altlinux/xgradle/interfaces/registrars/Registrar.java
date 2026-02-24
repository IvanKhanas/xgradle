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
package org.altlinux.xgradle.interfaces.registrars;

import java.util.List;

/**
 * Interface for artifact registration operations.
 * Defines contract for registering artifacts with external systems.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@FunctionalInterface
public interface Registrar {

    /**
     * Registers artifacts from the specified directory using the given command.
     *
     * @param searchingDir the directory to search for artifacts
     * @param registerCommand the command to use for registration
     * @param artifactNames list of artifact names to filter by (null or empty to process all)
     */
    void registerArtifacts(String searchingDir, String registerCommand, List<String> artifactNames);
}
