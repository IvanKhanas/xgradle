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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable snapshot of declared dependency usage across configurations.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public final class ConfigurationInfoSnapshot {

    private final Map<String, Set<ConfigurationInfo>> dependencyConfigurations;
    private final Map<String, Boolean> testDependencyFlags;
    private final Map<String, Set<String>> dependencyConfigNames;

    public ConfigurationInfoSnapshot(
            Map<String, Set<ConfigurationInfo>> dependencyConfigurations,
            Map<String, Boolean> testDependencyFlags,
            Map<String, Set<String>> dependencyConfigNames
    ) {
        this.dependencyConfigurations = deepImmutableConfigurationInfoMap(dependencyConfigurations);
        this.testDependencyFlags = Collections.unmodifiableMap(new LinkedHashMap<>(testDependencyFlags));
        this.dependencyConfigNames = deepImmutableStringSetMap(dependencyConfigNames);
    }

    public Map<String, Set<ConfigurationInfo>> getDependencyConfigurations() {
        return dependencyConfigurations;
    }

    public Map<String, Boolean> getTestDependencyFlags() {
        return testDependencyFlags;
    }

    public Map<String, Set<String>> getDependencyConfigNames() {
        return dependencyConfigNames;
    }

    private static Map<String, Set<ConfigurationInfo>> deepImmutableConfigurationInfoMap(
            Map<String, Set<ConfigurationInfo>> source
    ) {
        return Collections.unmodifiableMap(
                source.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> Set.copyOf(entry.getValue()),
                                (left, right) -> right,
                                LinkedHashMap::new
                        ))
        );
    }

    private static Map<String, Set<String>> deepImmutableStringSetMap(
            Map<String, Set<String>> source
    ) {
        return Collections.unmodifiableMap(
                source.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> Set.copyOf(entry.getValue()),
                                (left, right) -> right,
                                LinkedHashMap::new
                        ))
        );
    }
}
