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
package org.altlinux.xgradle.interfaces.processors;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds results of BOM processing.
 */
public final class BomResult {
    private final Map<String, List<String>> bomManagedDeps;
    private final Map<String, String> managedVersions;
    private final Set<String> processedBoms;

    public BomResult(
            Map<String, List<String>> bomManagedDeps,
            Map<String, String> managedVersions,
            Set<String> processedBoms
    ) {
        this.bomManagedDeps = immutableMap(bomManagedDeps);
        this.managedVersions = immutableMap(managedVersions);
        this.processedBoms = immutableSet(processedBoms);
    }

    public static BomResult empty() {
        return new BomResult(Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet());
    }

    public Map<String, List<String>> getBomManagedDeps() {
        return bomManagedDeps;
    }

    public Map<String, String> getManagedVersions() {
        return managedVersions;
    }

    public Set<String> getProcessedBoms() {
        return processedBoms;
    }

    private static <K, V> Map<K, V> immutableMap(Map<K, V> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<K, V> copy = (input instanceof LinkedHashMap)
                ? new LinkedHashMap<>(input)
                : new HashMap<>(input);
        return Collections.unmodifiableMap(copy);
    }

    private static <T> Set<T> immutableSet(Set<T> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(input));
    }
}
