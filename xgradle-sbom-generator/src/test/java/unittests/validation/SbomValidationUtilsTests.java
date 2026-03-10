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
package unittests.validation;

import org.altlinux.xgradle.impl.validation.SbomValidationUtils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SBOM validation utils")
class SbomValidationUtilsTests {

    @Test
    @DisplayName("Returns defaults for blank project info")
    void returnsDefaultsForBlankProjectInfo() {
        assertEquals("xgradle-project", SbomValidationUtils.requireProjectNameOrDefault(" "));
        assertEquals("unspecified", SbomValidationUtils.requireProjectVersionOrDefault(null));
    }

    @Test
    @DisplayName("Normalizes nullable values")
    void normalizesNullableValues() {
        assertEquals("value", SbomValidationUtils.normalizeNullable("  value  "));
        assertNull(SbomValidationUtils.normalizeNullable("   "));
        assertNull(SbomValidationUtils.normalizeNullable(null));
    }

    @Test
    @DisplayName("Require non-null returns value")
    void requireNonNullReturnsValue() {
        String value = SbomValidationUtils.requireNonNull("ok", "must not be null");
        assertEquals("ok", value);
    }

    @Test
    @DisplayName("Require non-null throws on null value")
    void requireNonNullThrowsOnNullValue() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> SbomValidationUtils.requireNonNull(null, "must not be null")
        );
        assertEquals("must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Requires non-null output path")
    void requiresNonNullOutputPath() {
        Path path = Path.of("build/reports/xgradle/sbom-spdx.json");
        assertSame(path, SbomValidationUtils.requireOutputPath(path));
        assertThrows(NullPointerException.class, () -> SbomValidationUtils.requireOutputPath(null));
    }
}
