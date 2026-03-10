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
package unittests.licenses;

import org.altlinux.xgradle.impl.models.SbomLicense;
import org.altlinux.xgradle.interfaces.licenses.SpdxLicenseMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import unittests.AbstractSbomModuleTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SPDX license mapper")
class SpdxLicenseMapperTests extends AbstractSbomModuleTest {

    private final SpdxLicenseMapper mapper = injector.getInstance(SpdxLicenseMapper.class);

    @Test
    @DisplayName("Resolves SPDX identifier from SPDX id")
    void resolvesIdentifierFromSpdxId() {
        String resolved = mapper.resolve(new SbomLicense("Apache-2.0", null)).orElse(null);
        assertEquals("Apache-2.0", resolved);
    }

    @Test
    @DisplayName("Resolves SPDX identifier from official license name")
    void resolvesIdentifierFromOfficialName() {
        String resolved = mapper.resolve(new SbomLicense("BSD Zero Clause License", null)).orElse(null);
        assertEquals("0BSD", resolved);
    }

    @Test
    @DisplayName("Resolves SPDX identifier from license URL")
    void resolvesIdentifierFromLicenseUrl() {
        String resolved = mapper.resolve(
                new SbomLicense(null, "http://spdx.org/licenses/0BSD.html?source=test#fragment")
        ).orElse(null);
        assertEquals("0BSD", resolved);
    }

    @Test
    @DisplayName("Resolves SPDX identifier from verbose non-SPDX name")
    void resolvesIdentifierFromVerboseName() {
        String resolved = mapper.resolve(
                new SbomLicense("The Apache Software License, Version 2.0", null)
        ).orElse(null);
        assertEquals("Apache-2.0", resolved);
    }

    @Test
    @DisplayName("Returns empty for unknown license")
    void returnsEmptyForUnknownLicense() {
        assertTrue(mapper.resolve(new SbomLicense("Unknown License Foo", null)).isEmpty());
    }
}
