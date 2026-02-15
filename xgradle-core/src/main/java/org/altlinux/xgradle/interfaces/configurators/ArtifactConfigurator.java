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
package org.altlinux.xgradle.interfaces.configurators;

import org.altlinux.xgradle.impl.model.ConfigurationInfo;
import org.altlinux.xgradle.impl.enums.MavenScope;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.gradle.api.invocation.Gradle;

import java.util.Map;
import java.util.Set;
/**
  * Configures artifact.

 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

public interface ArtifactConfigurator {
/**
  * Method the operation.

 */

    void configure(
            Gradle gradle,
            Map<String, MavenCoordinate> systemArtifacts,
            Map<String, Set<String>> dependencyConfigNames,
            Map<String, Set<ConfigurationInfo>> dependencyConfigurations,
            Set<String> testContextDependencies,
            Map<String, MavenScope> dependencyScopes
    );
/**
  * Returns configuration artifacts.

 */

    Map<String, Set<String>> getConfigurationArtifacts();
}
