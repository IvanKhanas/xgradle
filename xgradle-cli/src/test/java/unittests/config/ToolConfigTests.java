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
package unittests.config;

import org.altlinux.xgradle.impl.cli.CliArgumentsContainer;
import org.altlinux.xgradle.impl.config.ToolConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ToolConfig contract")
class ToolConfigTests {

    @Mock
    private CliArgumentsContainer arguments;

    /**
     * Ensures ToolConfig rejects null CliArgumentsContainer.
     */
    @Test
    @DisplayName("Constructor rejects null arguments")
    void constructorRejectsNullArguments() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new ToolConfig(null));
        assertEquals("Cli arguments can not be null", ex.getMessage());
    }

    /**
     * Ensures getExcludedArtifacts delegates to CliArgumentsContainer.getExcludedArtifact().
     */
    @Test
    @DisplayName("getExcludedArtifacts delegates to arguments")
    void getExcludedArtifactsDelegatesToArguments() {
        List<String> expected = List.of("foo", "bar");
        when(arguments.getExcludedArtifact()).thenReturn(expected);

        ToolConfig config = new ToolConfig(arguments);

        assertSame(expected, config.getExcludedArtifacts());
        verify(arguments).getExcludedArtifact();
        verifyNoMoreInteractions(arguments);
    }

    /**
     * Ensures isAllowSnapshots delegates to CliArgumentsContainer.hasAllowSnapshots().
     */
    @Test
    @DisplayName("isAllowSnapshots delegates to arguments")
    void isAllowSnapshotsDelegatesToArguments() {
        when(arguments.hasAllowSnapshots()).thenReturn(true);

        ToolConfig config = new ToolConfig(arguments);

        assertTrue(config.isAllowSnapshots());
        verify(arguments).hasAllowSnapshots();
        verifyNoMoreInteractions(arguments);
    }

    /**
     * Ensures getRemoveParentPoms delegates to CliArgumentsContainer.getRemoveParentPoms().
     */
    @Test
    @DisplayName("getRemoveParentPoms delegates to arguments")
    void getRemoveParentPomsDelegatesToArguments() {
        List<String> expected = List.of("a.pom", "b.pom");
        when(arguments.getRemoveParentPoms()).thenReturn(expected);

        ToolConfig config = new ToolConfig(arguments);

        assertSame(expected, config.getRemoveParentPoms());
        verify(arguments).getRemoveParentPoms();
        verifyNoMoreInteractions(arguments);
    }

    /**
     * Ensures getInstallPrefix delegates to CliArgumentsContainer.getInstallPrefix().
     */
    @Test
    @DisplayName("getInstallPrefix delegates to arguments")
    void getInstallPrefixDelegatesToArguments() {
        when(arguments.getInstallPrefix()).thenReturn("/buildroot");

        ToolConfig config = new ToolConfig(arguments);

        assertEquals("/buildroot", config.getInstallPrefix());
        verify(arguments).getInstallPrefix();
        verifyNoMoreInteractions(arguments);
    }

    /**
     * Ensures ToolConfig returns null if the underlying argument is null (passthrough behavior).
     */
    @Test
    @DisplayName("Null values are passed through as-is")
    void nullValuesArePassedThroughAsIs() {
        when(arguments.getExcludedArtifact()).thenReturn(null);
        when(arguments.getRemoveParentPoms()).thenReturn(null);
        when(arguments.getInstallPrefix()).thenReturn(null);

        ToolConfig config = new ToolConfig(arguments);

        assertAll(
                () -> assertNull(config.getExcludedArtifacts()),
                () -> assertNull(config.getRemoveParentPoms()),
                () -> assertNull(config.getInstallPrefix())
        );

        verify(arguments).getExcludedArtifact();
        verify(arguments).getRemoveParentPoms();
        verify(arguments).getInstallPrefix();
        verifyNoMoreInteractions(arguments);
    }
}
