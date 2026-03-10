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

package org.altlinux.xgradle.impl.model;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
/**
 * Specification for Dependency.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

public final class DependencySpec {

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String scope;

    private DependencySpec(String groupId, String artifactId, String version, String scope) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scope = scope;
    }

    public static DependencySpec parse(String coords) {
        Objects.requireNonNull(coords, "coords");

        String trimmed = coords.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("coords is empty");
        }

        String[] parts = trimmed.split(":", -1);
        if (parts.length < 2 || parts.length > 4) {
            throw new IllegalArgumentException("Invalid coords format. Expected groupId:artifactId[:version[:scope]]");
        }

        String g = parts[0].trim();
        String a = parts[1].trim();
        if (g.isEmpty() || a.isEmpty()) {
            throw new IllegalArgumentException("groupId and artifactId must be non-empty");
        }

        String v = parts.length >= 3 ? normalizeOpt(parts[2]) : null;
        String s = parts.length == 4 ? normalizeOpt(parts[3]) : null;

        return new DependencySpec(g, a, v, s);
    }

    private static String normalizeOpt(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

    public Optional<String> getScope() {
        return Optional.ofNullable(scope);
    }

    @Override
    public String toString() {
        return String.join(":", Arrays.asList(
                groupId,
                artifactId,
                version == null ? "" : version,
                scope == null ? "" : scope
        ));
    }
}
