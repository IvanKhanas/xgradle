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
package org.altlinux.xgradle.interfaces.writers;

import com.google.gson.JsonObject;

import java.nio.file.Path;

/**
 * Persists generated SBOM JSON documents.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface SbomOutputWriter {

    /**
     * Writes generated SBOM JSON report to the target output path.
     *
     * @param outputPath target report file path
     * @param report generated SBOM JSON object
     */
    void write(Path outputPath, JsonObject report);
}
