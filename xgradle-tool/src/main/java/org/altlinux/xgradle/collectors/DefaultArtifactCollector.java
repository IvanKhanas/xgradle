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
package org.altlinux.xgradle.collectors;

import com.google.inject.Inject;

import com.google.inject.name.Named;
import org.altlinux.xgradle.ProcessingType;
import org.altlinux.xgradle.api.collectors.ArtifactCollector;
import org.altlinux.xgradle.api.processors.PomProcessor;

import java.nio.file.Path;
import java.util.HashMap;

import java.util.Optional;

public class DefaultArtifactCollector implements ArtifactCollector {
    private final PomProcessor<HashMap<String, Path>> libraryPomProcessor;
    private final PomProcessor<HashMap<String, Path>> gradlePlugins;

    @Inject
    public DefaultArtifactCollector(@Named("Library") PomProcessor<HashMap<String, Path>> libraryPomProcessor,
    @Named("gradlePlugins") PomProcessor<HashMap<String, Path>> gradlePlugins
    ) {
        this.libraryPomProcessor = libraryPomProcessor;
        this.gradlePlugins = gradlePlugins;
    }

    @Override
    public HashMap<String,Path> collect(String searchingDir, Optional<String> artifactName, ProcessingType processingType) {
        if (artifactName.isPresent()) {
            if (processingType.equals(ProcessingType.PLUGINS)) {
                return gradlePlugins.process().pomsFromDirectory(searchingDir, artifactName);
            }
            else if (processingType.equals(ProcessingType.LIBRARY)){
            return libraryPomProcessor.process().pomsFromDirectory(searchingDir, artifactName);
        }
    }else if (processingType.equals(ProcessingType.LIBRARY)) {
            return libraryPomProcessor.process().pomsFromDirectory(searchingDir, Optional.empty());
        }
        return new HashMap<>();
    }
}
