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
package unittests.services;

import org.altlinux.xgradle.interfaces.services.PomMetadataLicense;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("PomMetadataLicense contract")
class PomMetadataLicenseTests {

    @Test
    @DisplayName("Normalizes non-empty values")
    void normalizesNonEmptyValues() {
        PomMetadataLicense license = new PomMetadataLicense(" Apache-2.0 ", " https://apache.org ");

        assertAll(
                () -> assertEquals("Apache-2.0", license.getName()),
                () -> assertEquals("https://apache.org", license.getUrl())
        );
    }

    @Test
    @DisplayName("Converts null and blank values to null")
    void convertsNullAndBlankValuesToNull() {
        PomMetadataLicense license = new PomMetadataLicense("   ", null);

        assertAll(
                () -> assertNull(license.getName()),
                () -> assertNull(license.getUrl())
        );
    }
}
