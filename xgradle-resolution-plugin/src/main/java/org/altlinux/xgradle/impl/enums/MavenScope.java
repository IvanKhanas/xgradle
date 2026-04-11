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

import java.util.Arrays;

/**
 * Maven dependency scope.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public enum MavenScope {

    COMPILE("compile"),

    RUNTIME("runtime"),

    PROVIDED("provided"),

    TEST("test");

    private final String scope;

    MavenScope(String scope) {
        this.scope = scope;
    }

    public String getScope() {
        return scope;
    }

    public static MavenScope fromScope(String value) {
        if (value == null || value.isBlank()) {
            return COMPILE;
        }

        String normalizedScope = value.trim().toLowerCase();
        return Arrays.stream(values())
                .filter(candidate -> candidate.scope.equals(normalizedScope))
                .findFirst()
                .orElse(COMPILE);
    }
}
