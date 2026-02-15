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
package org.altlinux.xgradle.impl.containers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.impl.enums.ProcessingType;
import org.altlinux.xgradle.interfaces.collectors.ArtifactCollector;
import org.altlinux.xgradle.interfaces.containers.ArtifactContainer;

import java.util.List;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

import java.nio.file.Path;

/**
 * Default implementation of ArtifactContainer for managing artifact collections.
 * Implements {@link ArtifactContainer}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultArtifactContainer implements ArtifactContainer {
    private final ArtifactCollector artifactCollector;

    @Inject
    DefaultArtifactContainer(ArtifactCollector artifactCollector) {
        this.artifactCollector = artifactCollector;
    }

    @Override
    public HashMap<String, Path> getArtifacts(String searchingDirectory, Optional<List<String>> artifactNames, ProcessingType processingType) {
        if (artifactNames.isPresent()) {
            return artifactCollector.collect(searchingDirectory, artifactNames, processingType);
        } else {
            return artifactCollector.collect(searchingDirectory, Optional.empty(), processingType);
        }
    }

    @Override
    public Collection<Path> getArtifactPaths(String searchingDirectory, Optional<List<String>> artifactNames, ProcessingType processingType) {
        if (artifactNames.isPresent()) {
            return artifactCollector.collect(searchingDirectory,artifactNames, processingType).values();
        } else {
            return artifactCollector.collect(searchingDirectory, Optional.empty(), processingType).values();
        }
    }

    @Override
    public Collection<String> getArtifactSignatures(String searchingDirectory, Optional<List<String>> artifactNames, ProcessingType processingType) {
        if (artifactNames.isPresent()) {
            return artifactCollector.collect(searchingDirectory, artifactNames, processingType).keySet();
        } else {
            return artifactCollector.collect(searchingDirectory, Optional.empty(), processingType).keySet();
        }
    }
}
