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
package unittests.preprocessors;

import org.altlinux.xgradle.impl.models.SbomComponent;
import org.altlinux.xgradle.interfaces.preprocessors.SbomComponentPreprocessor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import unittests.AbstractSbomModuleTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SBOM component preprocessor")
class SbomComponentPreprocessorTests extends AbstractSbomModuleTest {

    private final SbomComponentPreprocessor preprocessor = injector.getInstance(SbomComponentPreprocessor.class);

    @Test
    @DisplayName("Sorts components by group artifact version and file name")
    void sortsComponentsByContractOrder() {
        List<SbomComponent> input = List.of(
                SbomComponent.maven("b.group", "b-art", "1"),
                SbomComponent.maven("a.group", "z-art", "2"),
                SbomComponent.file("z.jar"),
                SbomComponent.maven("a.group", "a-art", "2"),
                SbomComponent.maven("a.group", "a-art", "1")
        );

        List<SbomComponent> ordered = preprocessor.preprocess(input);
        List<String> orderedKeys = new ArrayList<>();
        for (SbomComponent component : ordered) {
            orderedKeys.add(component.uniqueKey());
        }

        assertEquals(
                List.of(
                        "file:z.jar",
                        "a.group:a-art:1",
                        "a.group:a-art:2",
                        "a.group:z-art:2",
                        "b.group:b-art:1"
                ),
                orderedKeys
        );
    }

    @Test
    @DisplayName("Returns empty list for null input")
    void returnsEmptyListForNullInput() {
        List<SbomComponent> ordered = preprocessor.preprocess(null);
        assertTrue(ordered.isEmpty());
    }

    @Test
    @DisplayName("Returns new ordered list without mutating source collection")
    void returnsNewOrderedListWithoutMutatingSource() {
        List<SbomComponent> source = new ArrayList<>();
        source.add(SbomComponent.maven("z.group", "z-art", "2"));
        source.add(SbomComponent.maven("a.group", "a-art", "1"));

        List<SbomComponent> ordered = preprocessor.preprocess(source);

        assertNotSame(source, ordered);
        assertEquals("z.group:z-art:2", source.get(0).uniqueKey());
        assertEquals("a.group:a-art:1", source.get(1).uniqueKey());
        assertEquals("a.group:a-art:1", ordered.get(0).uniqueKey());
        assertEquals("z.group:z-art:2", ordered.get(1).uniqueKey());
    }
}
