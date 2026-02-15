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
package unittests.processors;

import org.altlinux.xgradle.interfaces.processors.TransitiveResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("TransitiveResult contract")
class TransitiveResultTests {

    @Test
    @DisplayName("Copies inputs and exposes immutable views")
    void copiesAndIsImmutable() {
        Set<String> main = new HashSet<>(Set.of("a"));
        Set<String> test = new HashSet<>(Set.of("b"));
        Set<String> skipped = new HashSet<>(Set.of("c"));

        TransitiveResult result = new TransitiveResult(main, test, skipped);

        main.clear();
        test.clear();
        skipped.clear();

        assertTrue(result.getMainDependencies().contains("a"));
        assertTrue(result.getTestDependencies().contains("b"));
        assertTrue(result.getSkippedDependencies().contains("c"));

        assertThrows(UnsupportedOperationException.class, () -> result.getMainDependencies().add("x"));
    }

    @Test
    @DisplayName("Empty() returns empty collections")
    void emptyReturnsEmpty() {
        TransitiveResult empty = TransitiveResult.empty();
        assertTrue(empty.getMainDependencies().isEmpty());
        assertTrue(empty.getTestDependencies().isEmpty());
        assertTrue(empty.getSkippedDependencies().isEmpty());
    }
}
