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
package unittests.caches;

import com.google.common.collect.ImmutableList;
import org.altlinux.xgradle.impl.caches.DefaultPomDataCache;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.interfaces.caches.PomDataCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("PomDataCache contract")
class PomDataCacheTests {

    @Test
    @DisplayName("Stores and retrieves pom entries, invalidate clears")
    void storesAndInvalidatesPom() {
        PomDataCache cache = new DefaultPomDataCache();
        MavenCoordinate coord = MavenCoordinate.builder()
                .groupId("g")
                .artifactId("a")
                .version("1")
                .build();

        assertNull(cache.getPom("k"));
        cache.putPom("k", coord);
        assertEquals(coord, cache.getPom("k"));

        cache.invalidatePom("k");
        assertNull(cache.getPom("k"));
    }

    @Test
    @DisplayName("Skips empty dependency/property caches")
    void skipsEmptyCollections() {
        PomDataCache cache = new DefaultPomDataCache();

        cache.putDependencies("deps", ImmutableList.of());
        cache.putDependencyManagement("dep-mgmt", ImmutableList.of());
        cache.putProperties("props", Map.of());

        assertNull(cache.getDependencies("deps"));
        assertNull(cache.getDependencyManagement("dep-mgmt"));
        assertNull(cache.getProperties("props"));
    }
}
