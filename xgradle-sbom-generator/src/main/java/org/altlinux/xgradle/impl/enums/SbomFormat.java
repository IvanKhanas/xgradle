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
package org.altlinux.xgradle.impl.enums;

import org.altlinux.xgradle.impl.validation.SbomValidationUtils;

import java.util.Arrays;
import java.util.Optional;

/**
 * Supported SBOM output formats.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public enum SbomFormat {
    SPDX("spdx"),
    CYCLONEDX("cyclonedx");

    private final String propertyValue;

    SbomFormat(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public String getFileSuffix() {
        return propertyValue;
    }

    public static Optional<SbomFormat> fromProperty(String value) {
        String normalized = SbomValidationUtils.normalizeNullable(value);
        if (normalized == null) {
            return Optional.empty();
        }

        normalized = normalized.toLowerCase(SbomValidationUtils.ROOT_LOCALE);
        if ("cyclondx".equals(normalized) || "cyclone-dx".equals(normalized)) {
            normalized = "cyclonedx";
        }

        final String normalizedValue = normalized;
        return Arrays.stream(values())
                .filter(format -> format.propertyValue.equals(normalizedValue))
                .findFirst();
    }
}
