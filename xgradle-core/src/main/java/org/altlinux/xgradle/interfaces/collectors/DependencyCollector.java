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

package org.altlinux.xgradle.interfaces.collectors;

import org.gradle.api.invocation.Gradle;

import java.util.Map;
import java.util.Set;

/**
 * Collects and aggregates dependency information from all projects in a Gradle build.
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface DependencyCollector extends Collector<Gradle, Set<String>> {

    /**
     * Collects declared dependencies from the Gradle build.
     *
     * @param gradle Gradle instance
     * @return set of dependency coordinates
     */
    @Override
    Set<String> collect(Gradle gradle);

    /**
     * Returns requested versions for collected dependencies.
     *
     * @return map of dependency key to requested versions
     */
    Map<String, Set<String>> getRequestedVersions();
}
