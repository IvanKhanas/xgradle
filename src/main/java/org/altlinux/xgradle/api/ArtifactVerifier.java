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
package org.altlinux.xgradle.api;

import org.altlinux.xgradle.model.MavenCoordinate;

import org.gradle.api.logging.Logger;

/**
 * Interface for classes that check properties of artifacts.
 *
 * @author Ivan Khanas
 */
public interface ArtifactVerifier {
    /**
     * Checks the existence of an artifact.
     *
     * @param coord maven coordinates
     * @param logger logger for diagnostics errors
     *
     * @return true if artefact exists and false otherwise
     */
    boolean verifyArtifactExists(MavenCoordinate coord, Logger logger);
}