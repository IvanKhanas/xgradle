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
package org.altlinux.xgradle.api.model;

import org.apache.maven.model.Model;

import java.nio.file.Path;

/**
 * Interface for artifact data container including Maven model and file paths.
 * Used to track processed artifacts and prevent duplicates.
 *
 * @author Ivan Khanas
 */
public interface ArtifactData {

    /**
     * Gets the artifact coordinates.
     *
     * @return artifact coordinates
     */
    ArtifactCoordinates getCoordinates();

    /**
     * Gets the Maven model of the artifact.
     *
     * @return Maven model
     */
    Model getModel();

    /**
     * Gets the path to the POM file.
     *
     * @return POM file path
     */
    Path getPomPath();

    /**
     * Gets the path to the JAR file.
     *
     * @return JAR file path, or null if not applicable (e.g., for BOM files)
     */
    Path getJarPath();

    /**
     * Checks if this artifact data represents the same artifact as another.
     * Implementation should compare based on coordinates.
     *
     * @param obj the object to compare with
     * @return true if artifacts have same coordinates, false otherwise
     */
    @Override
    boolean equals(Object obj);

    /**
     * Returns hash code based on artifact coordinates.
     *
     * @return hash code
     */
    @Override
    int hashCode();
}