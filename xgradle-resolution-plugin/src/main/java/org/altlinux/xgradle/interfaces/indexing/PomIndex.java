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

package org.altlinux.xgradle.interfaces.indexing;

import org.altlinux.xgradle.impl.model.MavenCoordinate;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
/**
  * Defines POM operations.

 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

public interface PomIndex {
/**
  * Builds a value.

 */

    void build(Path rootDirectory);
/**
  * Builds a value.

 */

    void build(List<Path> pomFiles);
/**
  * Finds the target.

 */

    Optional<MavenCoordinate> find(String groupId, String artifactId);
/**
  * Finds all for group.

 */

    List<MavenCoordinate> findAllForGroup(String groupId);
/**
  * Snapshot the operation.

 */

    Map<String, MavenCoordinate> snapshot();
}
