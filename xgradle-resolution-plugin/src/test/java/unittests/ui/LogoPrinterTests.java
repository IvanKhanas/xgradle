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
package unittests.ui;

import org.altlinux.xgradle.impl.utils.ui.LogoPrinter;
import org.gradle.StartParameter;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LogoPrinter contract")
class LogoPrinterTests {

    @Mock
    private Gradle gradle;

    @Mock
    private StartParameter startParameter;

    @Mock
    private Project rootProject;

    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        System.clearProperty("disable.logo");
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.clearProperty("disable.logo");
    }

    @Test
    @DisplayName("isLogoEnabled: returns true when property not set and gradle is null")
    void isLogoEnabledReturnsTrueWhenPropertyNotSet() {
        assertTrue(LogoPrinter.isLogoEnabled());
    }

    @Test
    @DisplayName("isLogoEnabled: returns false when disable.logo=true")
    void isLogoEnabledReturnsFalseWhenPropertySet() {
        System.setProperty("disable.logo", "true");
        assertFalse(LogoPrinter.isLogoEnabled());
    }

    @Test
    @DisplayName("isLogoEnabled(null): returns true when disable.logo not set")
    void isLogoEnabledWithNullGradleReturnsTrueByDefault() {
        assertTrue(LogoPrinter.isLogoEnabled(null));
    }

    @Test
    @DisplayName("isLogoEnabled(gradle): returns false when gradle projectDir is buildSrc")
    void isLogoEnabledReturnsFalseForBuildSrcProject() {
        when(gradle.getStartParameter()).thenReturn(startParameter);
        when(startParameter.getProjectDir()).thenReturn(new File("/some/path/buildSrc"));

        assertFalse(LogoPrinter.isLogoEnabled(gradle));
    }

    @Test
    @DisplayName("isLogoEnabled(gradle): returns false when gradle currentDir is buildSrc")
    void isLogoEnabledReturnsFalseWhenCurrentDirIsBuildSrc() {
        when(gradle.getStartParameter()).thenReturn(startParameter);
        when(startParameter.getProjectDir()).thenReturn(new File("/some/path"));
        when(startParameter.getCurrentDir()).thenReturn(new File("/some/path/buildSrc"));

        assertFalse(LogoPrinter.isLogoEnabled(gradle));
    }

    @Test
    @DisplayName("isLogoEnabled(gradle): returns true for normal non-buildSrc gradle")
    void isLogoEnabledReturnsTrueForNormalGradleBuild() {
        when(gradle.getStartParameter()).thenReturn(startParameter);
        when(startParameter.getProjectDir()).thenReturn(new File("/some/project"));
        when(startParameter.getCurrentDir()).thenReturn(new File("/some/project"));
        when(gradle.getRootProject()).thenReturn(rootProject);
        when(rootProject.getProjectDir()).thenReturn(new File("/some/project"));

        assertTrue(LogoPrinter.isLogoEnabled(gradle));
    }

    @Test
    @DisplayName("isLogoEnabled(gradle): returns false when rootProject dir contains buildSrc path")
    void isLogoEnabledReturnsFalseWhenRootProjectInBuildSrc() {
        when(gradle.getStartParameter()).thenReturn(startParameter);
        when(startParameter.getProjectDir()).thenReturn(new File("/some/path"));
        when(startParameter.getCurrentDir()).thenReturn(new File("/some/path"));
        when(gradle.getRootProject()).thenReturn(rootProject);
        when(rootProject.getProjectDir()).thenReturn(new File("/some/project/buildSrc/subproject"));

        assertFalse(LogoPrinter.isLogoEnabled(gradle));
    }

    @Test
    @DisplayName("isLogoEnabled(gradle): returns false when getRootProject throws exception")
    void isLogoEnabledHandlesRootProjectException() {
        when(gradle.getStartParameter()).thenReturn(startParameter);
        when(startParameter.getProjectDir()).thenReturn(new File("/some/path"));
        when(startParameter.getCurrentDir()).thenReturn(new File("/some/path"));
        when(gradle.getRootProject()).thenThrow(new RuntimeException("no root project"));

        assertTrue(LogoPrinter.isLogoEnabled(gradle));
    }

    @Test
    @DisplayName("printCenteredBanner: prints logo.txt content without throwing")
    void printCenteredBannerPrintsLogo() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        assertDoesNotThrow(LogoPrinter::printCenteredBanner);

        String output = out.toString();
        assertFalse(output.isEmpty(), "Banner output should not be empty");
    }

    @Test
    @DisplayName("printCenteredBanner: uses default width when console is unavailable")
    void printCenteredBannerUsesDefaultWidthWhenConsoleUnavailable() throws Exception {
        assumeTrue(System.console() == null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        LogoPrinter.printCenteredBanner();

        List<String> artLines = loadArtLines();
        int maxLength = artLines.stream()
                .mapToInt(String::length)
                .max()
                .orElse(0);
        int padding = Math.max(0, (80 - maxLength) / 2);
        String[] printedLines = out.toString(StandardCharsets.UTF_8).split("\\R");

        assertEquals(" ".repeat(padding) + artLines.get(0), printedLines[0]);
    }

    @Test
    @DisplayName("getTerminalWidth(hasConsole, columns): uses COLUMNS env when console is available")
    void getTerminalWidthUsesColumnsEnvWhenConsoleAvailable() throws Exception {
        assertEquals(120, invokeGetTerminalWidth(true, " 120 "));
    }

    @Test
    @DisplayName("getTerminalWidth(hasConsole, columns): throws for invalid COLUMNS")
    void getTerminalWidthUsesDefaultWidthForInvalidColumns() throws Exception {
        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> invokeGetTerminalWidth(true, "abc"));
        assertInstanceOf(NumberFormatException.class, exception.getCause());
    }

    @Test
    @DisplayName("getTerminalWidth(hasConsole, columns): uses default width for false console flag")
    void getTerminalWidthUsesDefaultWidthForNullConsole() throws Exception {
        assertEquals(80, invokeGetTerminalWidth(false, "120"));
    }

    @Test
    @DisplayName("getTerminalWidth(hasConsole, columns): uses default width for non-positive columns")
    void getTerminalWidthUsesDefaultWidthForNonPositiveColumns() throws Exception {
        assertEquals(80, invokeGetTerminalWidth(true, "0"));
    }

    private List<String> loadArtLines() throws Exception {
        try (InputStream inputStream = LogoPrinter.class.getClassLoader().getResourceAsStream("logo.txt")) {
            assertNotNull(inputStream);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.toList());
            }
        }
    }

    private int invokeGetTerminalWidth(boolean hasConsole, String columns) throws Exception {
        Method method = LogoPrinter.class.getDeclaredMethod("getTerminalWidth", boolean.class, String.class);
        method.setAccessible(true);
        return (int) method.invoke(null, hasConsole, columns);
    }
}
