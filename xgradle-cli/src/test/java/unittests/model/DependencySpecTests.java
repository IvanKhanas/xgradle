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
package unittests.model;

import org.altlinux.xgradle.impl.model.DependencySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("DependencySpec contract")
class DependencySpecTests {

    @Test
    @DisplayName("parse: groupId:artifactId returns spec with no version or scope")
    void parseTwoPartCoords() {
        DependencySpec spec = DependencySpec.parse("com.example:artifact");
        assertEquals("com.example", spec.getGroupId());
        assertEquals("artifact", spec.getArtifactId());
        assertTrue(spec.getVersion().isEmpty());
        assertTrue(spec.getScope().isEmpty());
    }

    @Test
    @DisplayName("parse: groupId:artifactId:version returns spec with version")
    void parseThreePartCoords() {
        DependencySpec spec = DependencySpec.parse("com.example:artifact:1.0");
        assertEquals("com.example", spec.getGroupId());
        assertEquals("artifact", spec.getArtifactId());
        assertEquals("1.0", spec.getVersion().orElseThrow());
        assertTrue(spec.getScope().isEmpty());
    }

    @Test
    @DisplayName("parse: full four-part coords returns spec with all fields")
    void parseFourPartCoords() {
        DependencySpec spec = DependencySpec.parse("com.example:artifact:1.0:compile");
        assertEquals("com.example", spec.getGroupId());
        assertEquals("artifact", spec.getArtifactId());
        assertEquals("1.0", spec.getVersion().orElseThrow());
        assertEquals("compile", spec.getScope().orElseThrow());
    }

    @Test
    @DisplayName("parse: empty version segment is treated as absent")
    void parseEmptyVersionSegment() {
        DependencySpec spec = DependencySpec.parse("com.example:artifact:");
        assertTrue(spec.getVersion().isEmpty());
    }

    @Test
    @DisplayName("parse: null coords throws NullPointerException")
    void parseNullThrowsNpe() {
        assertThrows(NullPointerException.class, () -> DependencySpec.parse(null));
    }

    @Test
    @DisplayName("parse: empty string throws IllegalArgumentException")
    void parseEmptyThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> DependencySpec.parse(""));
        assertThrows(IllegalArgumentException.class, () -> DependencySpec.parse("   "));
    }

    @Test
    @DisplayName("parse: only one segment throws IllegalArgumentException")
    void parseOneSegmentThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> DependencySpec.parse("com.example"));
    }

    @Test
    @DisplayName("parse: five segments throws IllegalArgumentException")
    void parseFiveSegmentsThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> DependencySpec.parse("a:b:c:d:e"));
    }

    @Test
    @DisplayName("parse: empty groupId throws IllegalArgumentException")
    void parseEmptyGroupIdThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> DependencySpec.parse(":artifact"));
    }

    @Test
    @DisplayName("parse: empty artifactId throws IllegalArgumentException")
    void parseEmptyArtifactIdThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> DependencySpec.parse("com.example:"));
    }

    @Test
    @DisplayName("toString: produces colon-separated representation")
    void toStringProducesColonSeparated() {
        DependencySpec spec = DependencySpec.parse("com.example:artifact:1.0:compile");
        assertEquals("com.example:artifact:1.0:compile", spec.toString());
    }

    @Test
    @DisplayName("toString: omitted optional fields produce empty segments")
    void toStringWithMissingOptionalFields() {
        DependencySpec spec = DependencySpec.parse("com.example:artifact");
        String s = spec.toString();
        assertTrue(s.startsWith("com.example:artifact"));
    }
}
