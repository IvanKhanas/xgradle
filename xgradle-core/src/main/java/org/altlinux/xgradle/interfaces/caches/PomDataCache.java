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
package org.altlinux.xgradle.interfaces.caches;

import com.google.common.collect.ImmutableList;

import org.altlinux.xgradle.impl.model.MavenCoordinate;

import org.gradle.api.logging.Logger;

import java.util.Map;
/**
  * Caches POM data.

 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

public interface PomDataCache {
/**
  * Returns POM.

 */
    MavenCoordinate getPom(String key);
/**
  * Put POM.

 */

    void putPom(String key, MavenCoordinate coordinate);
/**
  * Invalidate POM.

 */

    void invalidatePom(String key);
/**
  * Returns dependency management.

 */

    ImmutableList<MavenCoordinate> getDependencyManagement(String key);
/**
  * Put dependency management.

 */

    void putDependencyManagement(String key, ImmutableList<MavenCoordinate> dependencies);
/**
  * Returns dependencies.

 */

    ImmutableList<MavenCoordinate> getDependencies(String key);
/**
  * Put dependencies.

 */

    void putDependencies(String key, ImmutableList<MavenCoordinate> dependencies);
/**
  * Returns properties.

 */

    Map<String, String> getProperties(String key);
/**
  * Put properties.

 */

    void putProperties(String key, Map<String, String> properties);
/**
  * Log stats.

 */

    void logStats(Logger logger);
}
