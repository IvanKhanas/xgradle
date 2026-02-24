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
package org.altlinux.xgradle.interfaces.installers;

import org.altlinux.xgradle.impl.enums.ProcessingType;

import java.util.List;

/**
 * Interface for artifacts installation operations.
 * Defines contract for installing artifacts to target directories.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface ArtifactsInstaller {

    /**
     * Installs artifacts to the specified target directories.
     *
     * @param searchingDirectory the directory to search for artifacts
     * @param artifactNames list of artifact names to filter by (null or empty to process all)
     * @param pomInstallationDirectory target directory for POM files
     * @param jarInstallationDirectory target directory for JAR files
     * @param processingType the type of processing (LIBRARY or PLUGINS)
     */
    void install(String searchingDirectory,
                 List<String> artifactNames,
                 String pomInstallationDirectory,
                 String jarInstallationDirectory,
                 ProcessingType processingType);
}
