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
package org.altlinux.xgradle.impl.licenses;

import java.util.Map;

/**
 * Immutable lookup catalog for SPDX license resolution.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class SpdxLicenseCatalog {

    private final Map<String, String> idByIdentifier;
    private final Map<String, String> idByName;
    private final Map<String, String> idByReducedName;
    private final Map<String, String> idByUrl;

    SpdxLicenseCatalog(
            Map<String, String> idByIdentifier,
            Map<String, String> idByName,
            Map<String, String> idByReducedName,
            Map<String, String> idByUrl
    ) {
        this.idByIdentifier = Map.copyOf(idByIdentifier);
        this.idByName = Map.copyOf(idByName);
        this.idByReducedName = Map.copyOf(idByReducedName);
        this.idByUrl = Map.copyOf(idByUrl);
    }

    String byIdentifier(String key) {
        if (key == null) {
            return null;
        }
        return idByIdentifier.get(key);
    }

    String byName(String key) {
        if (key == null) {
            return null;
        }
        return idByName.get(key);
    }

    String byReducedName(String key) {
        if (key == null) {
            return null;
        }
        return idByReducedName.get(key);
    }

    String byUrl(String key) {
        if (key == null) {
            return null;
        }
        return idByUrl.get(key);
    }
}
