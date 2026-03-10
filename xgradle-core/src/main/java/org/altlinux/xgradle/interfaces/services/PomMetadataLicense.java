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
package org.altlinux.xgradle.interfaces.services;

/**
 * Represents a POM license entry.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public final class PomMetadataLicense {

    private final String name;
    private final String url;

    public PomMetadataLicense(String name, String url) {
        this.name = normalize(name);
        this.url = normalize(url);
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
