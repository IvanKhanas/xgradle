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
package unittests.enums;

import org.altlinux.xgradle.impl.enums.SbomFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@DisplayName("SBOM format parser")
class SbomFormatTests {

    @Test
    @DisplayName("Returns expected file suffixes")
    void returnsExpectedFileSuffixes() {
        assertEquals("spdx", SbomFormat.SPDX.getFileSuffix());
        assertEquals("cyclonedx", SbomFormat.CYCLONEDX.getFileSuffix());
    }

    @Test
    @DisplayName("Parses SPDX")
    void parsesSpdx() {
        assertEquals(SbomFormat.SPDX, SbomFormat.fromProperty("spdx").orElseThrow());
        assertEquals(SbomFormat.SPDX, SbomFormat.fromProperty("  SpDx ").orElseThrow());
    }

    @Test
    @DisplayName("Parses CycloneDX aliases")
    void parsesCycloneDxAliases() {
        assertEquals(SbomFormat.CYCLONEDX, SbomFormat.fromProperty("cyclonedx").orElseThrow());
        assertEquals(SbomFormat.CYCLONEDX, SbomFormat.fromProperty("cyclondx").orElseThrow());
        assertEquals(SbomFormat.CYCLONEDX, SbomFormat.fromProperty("Cyclone-DX").orElseThrow());
    }

    @Test
    @DisplayName("Returns empty for unsupported values")
    void returnsEmptyForUnsupportedFormat() {
        assertTrue(SbomFormat.fromProperty("xml").isEmpty());
        assertTrue(SbomFormat.fromProperty("").isEmpty());
        assertTrue(SbomFormat.fromProperty(null).isEmpty());
    }
}
