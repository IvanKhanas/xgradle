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
package unittests.resolvers;

import org.altlinux.xgradle.impl.enums.MavenScope;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.resolvers.DefaultArtifactResolver;
import org.altlinux.xgradle.interfaces.services.VersionScanner;
import org.gradle.api.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ArtifactResolver contract")
class ArtifactResolverTests {

    @Mock
    private VersionScanner scanner;

    @Mock
    private Logger logger;

    @Test
    @DisplayName("Resolve stores results and filter drops test/bom")
    void resolvesAndFilters() {
        DefaultArtifactResolver resolver = new DefaultArtifactResolver(scanner);

        MavenCoordinate normal = MavenCoordinate.builder()
                .groupId("g")
                .artifactId("lib")
                .version("1")
                .build();

        MavenCoordinate test = MavenCoordinate.builder()
                .groupId("g")
                .artifactId("test")
                .version("1")
                .scope(MavenScope.TEST)
                .build();

        MavenCoordinate bom = MavenCoordinate.builder()
                .groupId("g")
                .artifactId("bom")
                .version("1")
                .packaging("pom")
                .build();

        Map<String, MavenCoordinate> scanned = new HashMap<>();
        scanned.put("g:lib", normal);
        scanned.put("g:test", test);
        scanned.put("g:bom", bom);
        when(scanner.scanSystemArtifacts(Set.of("g:lib"))).thenReturn(scanned);
        when(scanner.getNotFoundDependencies()).thenReturn(Set.of("g:missing"));

        resolver.resolve(Set.of("g:lib"), logger);
        resolver.filter();

        assertTrue(resolver.getSystemArtifacts().containsKey("g:lib"));
        assertFalse(resolver.getSystemArtifacts().containsKey("g:test"));
        assertFalse(resolver.getSystemArtifacts().containsKey("g:bom"));
        assertEquals(Set.of("g:missing"), resolver.getNotFoundDependencies());
    }
}
