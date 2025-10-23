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
package org.altlinux.xgradle.model;

import org.altlinux.xgradle.api.model.ArtifactCoordinates;
import org.altlinux.xgradle.api.model.ArtifactData;

import org.apache.maven.model.Model;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Default implementation of ArtifactData container.
 * Stores artifact coordinates, Maven model, and file paths for processed artifacts.
 * Used to track processed artifacts and prevent duplicates.
 *
 * @author Ivan Khanas
 */
public class DefaultArtifactData implements ArtifactData {

    private final ArtifactCoordinates coordinates;
    private final Model model;
    private final Path pomPath;
    private final Path jarPath;

    /**
     * Constructs a new DefaultArtifactData with specified parameters.
     *
     * @param coordinates artifact coordinates
     * @param model Maven model
     * @param pomPath path to the POM file
     * @param jarPath path to the JAR file (may be null for BOM files)
     */
    public DefaultArtifactData(
            ArtifactCoordinates coordinates,
            Model model,
            Path pomPath,
            Path jarPath
    ) {
        this.coordinates = coordinates;
        this.model = model;
        this.pomPath = pomPath;
        this.jarPath = jarPath;
    }

    /**
     * Gets the artifact coordinates.
     *
     * @return artifact coordinates
     */
    public ArtifactCoordinates getCoordinates() {
        return coordinates;
    }

    /**
     * Gets the Maven model of the artifact.
     *
     * @return Maven model
     */
    public Model getModel() {
        return model;
    }

    /**
     * Gets the path to the POM file.
     *
     * @return POM file path
     */
    public Path getPomPath() {
        return pomPath;
    }

    /**
     * Gets the path to the JAR file.
     *
     * @return JAR file path, or null if not applicable
     */
    public Path getJarPath() {
        return jarPath;
    }

    /**
     * Checks if this artifact data represents the same artifact as another.
     * Comparison is based on artifact coordinates.
     *
     * @param o the object to compare with
     * @return true if artifacts have same coordinates, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultArtifactData that = (DefaultArtifactData) o;
        return Objects.equals(coordinates, that.coordinates);
    }

    /**
     * Returns hash code based on artifact coordinates.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(coordinates);
    }
}
