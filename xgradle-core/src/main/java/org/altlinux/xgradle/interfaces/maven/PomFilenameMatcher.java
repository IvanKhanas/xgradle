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

import java.nio.file.Path;
import java.util.Set;

/**
 * Interface for matching POM filenames to Maven artifacts and versions.
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface PomFilenameMatcher {

    /**
     * Checks whether the given path corresponds to a candidate POM file
     * for the specified artifact.
     */
    boolean matches(Path path, String variant, String artifactId);

    /**
     * Generates possible base-name variants for an artifact based on
     * its group ID and artifact ID.
     */
    Set<String> generateNameVariants(String groupId, String artifactId);
}