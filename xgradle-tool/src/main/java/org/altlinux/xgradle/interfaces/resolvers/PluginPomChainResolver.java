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
package org.altlinux.xgradle.interfaces.resolvers;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Resolves full POM chain for Gradle plugins (parent + pom dependencies).
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface PluginPomChainResolver {

    /**
     * Resolves full POM chain from selected POMs and mapped artifacts.
     *
     * @param searchingDirectory directory to search for POMs
     * @param artifactNames list of artifact names to filter by (null or empty to process all)
     * @param artifactsMap map of POM paths to artifacts
     * @return resolved POM chain result
     */
    PluginPomChainResult resolve(
            String searchingDirectory,
            List<String> artifactNames,
            Map<String, Path> artifactsMap
    );
}
