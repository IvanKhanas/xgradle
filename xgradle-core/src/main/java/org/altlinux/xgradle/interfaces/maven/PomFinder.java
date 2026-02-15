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
package org.altlinux.xgradle.interfaces.maven;

import org.altlinux.xgradle.impl.model.MavenCoordinate;

import java.util.List;

/**
 * Locates a POM that corresponds to a Maven artifact.
 * A finder only resolves which POM file should be used.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface PomFinder {

    /**
     * Finds a POM that matches the given Maven coordinates.
     *
     * @param groupId artifact groupId
     * @param artifactId artifactId
     *
     * @return resolved coordinates (with pomPath set) or null if not found
     */
    MavenCoordinate findPomForArtifact(String groupId, String artifactId);
/**
  * Finds all poms for group.

 */

    List<MavenCoordinate> findAllPomsForGroup(String groupId);
}
