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
package org.altlinux.xgradle.impl.maven;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.indexing.PomIndex;
import org.altlinux.xgradle.interfaces.maven.PomFinder;
import org.altlinux.xgradle.impl.extensions.SystemDepsExtension;
import org.altlinux.xgradle.impl.model.MavenCoordinate;

import org.gradle.api.logging.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
/**
 * Finder for Maven POM.
 * Implements {@link PomFinder}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

@Singleton
final class MavenPomFinder implements PomFinder {

    private final PomIndex pomIndex;
    private final Logger logger;

    @Inject
    MavenPomFinder(PomIndex pomIndex, Logger logger) {
        this.pomIndex = pomIndex;
        this.logger = logger;

        Path root = Paths.get(SystemDepsExtension.getPomsPath());
        try {
            this.pomIndex.build(root);
        } catch (RuntimeException e) {
            logger.lifecycle("Failed to build POM index from {}: {}", root, e.getMessage());
        }
    }

    @Override
    public MavenCoordinate findPomForArtifact(String groupId, String artifactId) {
        return pomIndex.find(groupId, artifactId).orElse(null);
    }

    @Override
    public List<MavenCoordinate> findAllPomsForGroup(String groupId) {
        return pomIndex.findAllForGroup(groupId);
    }
}
