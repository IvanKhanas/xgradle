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
package org.altlinux.xgradle.impl.model;

import org.altlinux.xgradle.interfaces.model.ArtifactCoordinates;
import org.altlinux.xgradle.interfaces.model.ArtifactData;

import org.apache.maven.model.Model;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Default implementation of ArtifactData container.
 * Implements {@link ArtifactData}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class DefaultArtifactData implements ArtifactData {

    private final ArtifactCoordinates coordinates;
    private final Model model;
    private final Path pomPath;
    private final Path jarPath;

    DefaultArtifactData(
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

    public ArtifactCoordinates getCoordinates() {
        return coordinates;
    }

    public Model getModel() {
        return model;
    }

    public Path getPomPath() {
        return pomPath;
    }

    public Path getJarPath() {
        return jarPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultArtifactData that = (DefaultArtifactData) o;
        return Objects.equals(coordinates, that.coordinates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coordinates);
    }
}
