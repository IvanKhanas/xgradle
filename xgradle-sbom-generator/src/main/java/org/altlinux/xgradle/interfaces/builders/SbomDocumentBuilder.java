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
package org.altlinux.xgradle.interfaces.builders;

import com.google.gson.JsonObject;
import org.altlinux.xgradle.impl.enums.SbomFormat;
import org.altlinux.xgradle.impl.models.SbomComponent;

import java.util.List;

/**
 * Builds a format-specific SBOM JSON document from normalized components.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface SbomDocumentBuilder {

    /**
     * Returns SBOM format supported by this builder implementation.
     *
     * @return supported SBOM format
     */
    SbomFormat format();

    /**
     * Builds a format-specific SBOM document from normalized project data.
     *
     * @param projectName project name used for top-level metadata
     * @param projectVersion project version used for top-level metadata
     * @param components normalized and preprocessed SBOM components
     * @return generated SBOM JSON payload
     */
    JsonObject build(
            String projectName,
            String projectVersion,
            List<SbomComponent> components
    );
}
