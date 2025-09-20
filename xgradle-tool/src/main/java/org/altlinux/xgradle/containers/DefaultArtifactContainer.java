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
package org.altlinux.xgradle.containers;

import com.google.inject.Inject;
import org.altlinux.xgradle.ProcessingType;
import org.altlinux.xgradle.api.collectors.ArtifactCollector;
import org.altlinux.xgradle.api.containers.ArtifactContainer;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

public class DefaultArtifactContainer implements ArtifactContainer {
    private final ArtifactCollector artifactCollector;

    @Inject
    public DefaultArtifactContainer(ArtifactCollector artifactCollector) {
        this.artifactCollector = artifactCollector;
    }

    @Override
    public HashMap<String, Path> getArtifacts(String searchingDirectory, Optional<String> artifactName, ProcessingType processingType) {
        if (artifactName.isPresent()) {
            return artifactCollector.collect(searchingDirectory, artifactName, processingType);
        } else {
            return artifactCollector.collect(searchingDirectory, Optional.empty(), processingType);
        }
    }

    @Override
    public Collection<Path> getArtifactPaths(String searchingDirectory, Optional<String> artifactName, ProcessingType processingType) {
        if (artifactName.isPresent()) {
            return artifactCollector.collect(searchingDirectory,artifactName, processingType).values();
        } else {
            return artifactCollector.collect(searchingDirectory, Optional.empty(), processingType).values();
        }
    }

    @Override
    public Collection<String> getArtifactSignatures(String searchingDirectory, Optional<String> artifactName, ProcessingType processingType) {
        if (artifactName.isPresent()) {
            return artifactCollector.collect(searchingDirectory, artifactName, processingType).keySet();
        } else {
            return artifactCollector.collect(searchingDirectory, Optional.empty(), processingType).keySet();
        }
    }
}
