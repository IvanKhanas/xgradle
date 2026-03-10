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
package org.altlinux.xgradle.interfaces.generators;

import org.altlinux.xgradle.impl.enums.SbomFormat;
import org.altlinux.xgradle.impl.models.SbomComponent;

import java.nio.file.Path;
import java.util.Collection;

/**
 * Generates SBOM documents in a supported format.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface SbomGenerator {

    /**
     * Generates SBOM report file for given project metadata and components.
     *
     * @param format output SBOM format
     * @param outputPath target file path for generated report
     * @param projectName project name to include into report metadata
     * @param projectVersion project version to include into report metadata
     * @param components project components to serialize into SBOM
     */
    void generate(
            SbomFormat format,
            Path outputPath,
            String projectName,
            String projectVersion,
            Collection<SbomComponent> components
    );
}
