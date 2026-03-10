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
import java.util.HashSet;
import java.util.Set;

/**
 * Holds dependency sets produced by transitive processing.
 */
public final class TransitiveResult {
    private final Set<String> mainDependencies;
    private final Set<String> testDependencies;
    private final Set<String> skippedDependencies;

    public TransitiveResult(
            Set<String> mainDependencies,
            Set<String> testDependencies,
            Set<String> skippedDependencies
    ) {
        this.mainDependencies = immutableCopy(mainDependencies);
        this.testDependencies = immutableCopy(testDependencies);
        this.skippedDependencies = immutableCopy(skippedDependencies);
    }

    public static TransitiveResult empty() {
        return new TransitiveResult(Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
    }

    public Set<String> getMainDependencies() {
        return mainDependencies;
    }

    public Set<String> getTestDependencies() {
        return testDependencies;
    }

    public Set<String> getSkippedDependencies() {
        return skippedDependencies;
    }

    private static Set<String> immutableCopy(Set<String> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(input));
    }
}
