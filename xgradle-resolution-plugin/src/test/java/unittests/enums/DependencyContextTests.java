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
package unittests.enums;

import org.altlinux.xgradle.impl.enums.DependencyContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("DependencyContext contract")
class DependencyContextTests {

    @Test
    @DisplayName("merge: first null returns second")
    void mergeFirstNullReturnsSecond() {
        assertSame(DependencyContext.MAIN, DependencyContext.merge(null, DependencyContext.MAIN));
    }

    @Test
    @DisplayName("merge: second null returns first")
    void mergeSecondNullReturnsFirst() {
        assertSame(DependencyContext.TEST, DependencyContext.merge(DependencyContext.TEST, null));
    }

    @Test
    @DisplayName("merge: both ALL returns ALL")
    void mergeBothAllReturnsAll() {
        assertSame(DependencyContext.ALL, DependencyContext.merge(DependencyContext.ALL, DependencyContext.ALL));
    }

    @Test
    @DisplayName("merge: same non-ALL returns same")
    void mergeSameReturnsFirst() {
        assertSame(DependencyContext.MAIN, DependencyContext.merge(DependencyContext.MAIN, DependencyContext.MAIN));
        assertSame(DependencyContext.TEST, DependencyContext.merge(DependencyContext.TEST, DependencyContext.TEST));
    }

    @Test
    @DisplayName("merge: different contexts returns ALL")
    void mergeDifferentReturnsAll() {
        assertSame(DependencyContext.ALL, DependencyContext.merge(DependencyContext.MAIN, DependencyContext.TEST));
        assertSame(DependencyContext.ALL, DependencyContext.merge(DependencyContext.TEST, DependencyContext.MAIN));
    }

    @Test
    @DisplayName("merge: MAIN and ALL returns ALL")
    void mergeMainAndAllReturnsAll() {
        assertSame(DependencyContext.ALL, DependencyContext.merge(DependencyContext.MAIN, DependencyContext.ALL));
        assertSame(DependencyContext.ALL, DependencyContext.merge(DependencyContext.ALL, DependencyContext.MAIN));
    }

    @Test
    @DisplayName("Values: enum has all expected constants")
    void enumHasExpectedValues() {
        DependencyContext[] values = DependencyContext.values();
        assertEquals(3, values.length);
        assertEquals(DependencyContext.MAIN, DependencyContext.valueOf("MAIN"));
        assertEquals(DependencyContext.TEST, DependencyContext.valueOf("TEST"));
        assertEquals(DependencyContext.ALL, DependencyContext.valueOf("ALL"));
    }
}
