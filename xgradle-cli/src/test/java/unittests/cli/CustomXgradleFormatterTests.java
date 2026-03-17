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
package unittests.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import org.altlinux.xgradle.impl.cli.CustomXgradleFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("CustomXgradleFormatter contract")
class CustomXgradleFormatterTests {

    @Test
    @DisplayName("Usage contains examples and program name")
    void usageContainsExamplesAndProgramName() {
        CustomXgradleFormatter formatter = newFormatter();
        StringBuilder out = new StringBuilder();

        formatter.usage(out);

        String content = out.toString();
        assertAll(
                () -> assertTrue(content.contains("Usage examples:")),
                () -> assertTrue(content.contains("Usage: xgradle-cli [options]")),
                () -> assertTrue(content.contains("Options:"))
        );
    }

    @Test
    @DisplayName("Sorts parameters by declared order")
    void sortsParametersByOrder() {
        CustomXgradleFormatter formatter = newFormatter();
        StringBuilder out = new StringBuilder();

        formatter.usage(out, "");

        String content = out.toString();
        int alpha = content.indexOf("--alpha");
        int omega = content.indexOf("--omega");

        assertAll(
                () -> assertTrue(alpha >= 0, "Expected --alpha in usage"),
                () -> assertTrue(omega >= 0, "Expected --omega in usage"),
                () -> assertTrue(alpha < omega, "Expected --alpha before --omega")
        );
    }

    @Test
    @DisplayName("Delegates all usage overloads and supports indent")
    void delegatesUsageOverloads() {
        CustomXgradleFormatter formatter = newFormatter();

        StringBuilder plain = new StringBuilder();
        formatter.usage("any-command", plain);

        StringBuilder indented = new StringBuilder();
        formatter.usage("any-command", indented, ">>");
        formatter.usage("any-command");

        assertAll(
                () -> assertTrue(plain.toString().contains("Usage examples:")),
                () -> assertTrue(indented.toString().contains(">>  --alpha")),
                () -> assertTrue(indented.toString().contains(">>  --omega"))
        );
    }

    @Test
    @DisplayName("Returns empty command description")
    void returnsEmptyCommandDescription() {
        CustomXgradleFormatter formatter = newFormatter();
        assertEquals("", formatter.getCommandDescription("anything"));
    }

    private CustomXgradleFormatter newFormatter() {
        JCommander commander = mock(JCommander.class);
        ParameterDescription omega = mock(ParameterDescription.class, RETURNS_DEEP_STUBS);
        ParameterDescription alpha = mock(ParameterDescription.class, RETURNS_DEEP_STUBS);

        when(commander.getProgramName()).thenReturn("xgradle-cli");
        when(commander.getParameters()).thenReturn(new ArrayList<>(List.of(omega, alpha)));

        when(alpha.getNames()).thenReturn("--alpha");
        when(alpha.getDescription()).thenReturn("Alpha option");
        when(alpha.getParameter().order()).thenReturn(10);

        when(omega.getNames()).thenReturn("--omega");
        when(omega.getDescription()).thenReturn("Omega option");
        when(omega.getParameter().order()).thenReturn(20);

        return new CustomXgradleFormatter(commander);
    }
}
