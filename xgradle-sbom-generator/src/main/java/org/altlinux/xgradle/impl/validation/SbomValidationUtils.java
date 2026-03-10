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
package org.altlinux.xgradle.impl.validation;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Shared validation and normalization utilities for SBOM generation.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public final class SbomValidationUtils {

    public static final Locale ROOT_LOCALE = Locale.ROOT;
    public static final String DEFAULT_PROJECT_NAME = "xgradle-project";
    public static final String DEFAULT_PROJECT_VERSION = "unspecified";

    private SbomValidationUtils() {
    }

    public static <T> T requireNonNull(
            T value,
            String message
    ) {
        return Objects.requireNonNull(value, message);
    }

    public static Path requireOutputPath(Path outputPath) {
        return requireNonNull(outputPath, "Output path must not be null");
    }

    public static String requireProjectNameOrDefault(String projectName) {
        String normalized = normalizeNullable(projectName);
        return normalized != null ? normalized : DEFAULT_PROJECT_NAME;
    }

    public static String requireProjectVersionOrDefault(String projectVersion) {
        String normalized = normalizeNullable(projectVersion);
        return normalized != null ? normalized : DEFAULT_PROJECT_VERSION;
    }

    public static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
