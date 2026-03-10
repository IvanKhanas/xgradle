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

import org.altlinux.xgradle.interfaces.processors.BomResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("BomResult contract")
class BomResultTests {

    @Test
    @DisplayName("Copies inputs and exposes immutable views")
    void copiesAndIsImmutable() {
        Map<String, List<String>> deps = new HashMap<>();
        deps.put("bom:1", List.of("g:a:1"));
        Map<String, String> versions = new HashMap<>();
        versions.put("g:a", "1");
        Set<String> processed = new HashSet<>(Set.of("bom"));

        BomResult result = new BomResult(deps, versions, processed);

        deps.clear();
        versions.clear();
        processed.clear();

        assertEquals(List.of("g:a:1"), result.getBomManagedDeps().get("bom:1"));
        assertEquals("1", result.getManagedVersions().get("g:a"));
        assertTrue(result.getProcessedBoms().contains("bom"));

        assertThrows(UnsupportedOperationException.class, () -> result.getManagedVersions().put("x", "y"));
        assertThrows(UnsupportedOperationException.class, () -> result.getProcessedBoms().add("x"));
    }

    @Test
    @DisplayName("Empty() returns empty collections")
    void emptyReturnsEmpty() {
        BomResult empty = BomResult.empty();
        assertTrue(empty.getBomManagedDeps().isEmpty());
        assertTrue(empty.getManagedVersions().isEmpty());
        assertTrue(empty.getProcessedBoms().isEmpty());
    }
}
