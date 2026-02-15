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

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.altlinux.xgradle.interfaces.cli.CommandLineParser;
import org.altlinux.xgradle.impl.cli.CliModule;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test command line parsing")
class CommandLineParserTests {

    private CommandLineParser parser;

    @BeforeEach
    void setUp() {
        Injector injector = Guice.createInjector(new CliModule());
        parser = injector.getInstance(CommandLineParser.class);
    }

    /**
     * Verifies that {@link CommandLineParser#parseCommandLine(String)} returns an empty list
     * when the input command string is {@code null}.
     */
    @Test
    @DisplayName("Returns empty list for null input")
    void returnsEmptyList_forNull() {
        assertEquals(List.of(), parser.parseCommandLine(null));
    }

    /**
     * Verifies that {@link CommandLineParser#parseCommandLine(String)} returns an empty list
     * when the input command string is blank (empty or whitespace-only).
     */
    @Test
    @DisplayName("Returns empty list for blank input")
    void returnsEmptyList_forBlank() {
        assertAll(
                () -> assertEquals(List.of(), parser.parseCommandLine("")),
                () -> assertEquals(List.of(), parser.parseCommandLine("   ")),
                () -> assertEquals(List.of(), parser.parseCommandLine("\t \n\r"))
        );
    }

    /**
     * Verifies that unquoted tokens are split by any whitespace and consecutive whitespace
     * does not produce empty tokens.
     */
    @Test
    @DisplayName("Splits by whitespace when no quotes")
    void splitsByWhitespace_noQuotes() {
        assertEquals(
                List.of("a", "bb", "ccc"),
                parser.parseCommandLine("a   bb\tccc")
        );
    }

    /**
     * Verifies that content inside double quotes is treated as a single token and
     * quote characters are not included in the result.
     */
    @Test
    @DisplayName("Supports double quotes (keeps spaces inside quotes)")
    void supportsDoubleQuotes() {
        assertEquals(
                List.of("cmd", "arg with spaces", "tail"),
                parser.parseCommandLine("cmd \"arg with spaces\" tail")
        );
    }

    /**
     * Verifies that content inside single quotes is treated as a single token and
     * quote characters are not included in the result.
     */
    @Test
    @DisplayName("Supports single quotes (keeps spaces inside quotes)")
    void supportsSingleQuotes() {
        assertEquals(
                List.of("cmd", "arg with spaces", "tail"),
                parser.parseCommandLine("cmd 'arg with spaces' tail")
        );
    }

    /**
     * Verifies that a command wrapped entirely in matching quotes has those outer quotes stripped
     * before parsing (wrapping quotes are not preserved as a token).
     */
    @Test
    @DisplayName("Removes wrapping quotes around the whole command")
    void stripsWrappingQuotes_wholeCommand() {
        assertAll(
                () -> assertEquals(List.of("cmd", "x", "y"), parser.parseCommandLine("\"cmd x y\"")),
                () -> assertEquals(List.of("cmd", "x", "y"), parser.parseCommandLine("'cmd x y'"))
        );
    }

    /**
     * Verifies that both single-quoted and double-quoted segments can appear in the same command
     * and are each parsed as single tokens.
     */
    @Test
    @DisplayName("Allows mixed quote groups in one command")
    void allowsMixedQuotes() {
        assertEquals(
                List.of("cmd", "a b", "c d", "e"),
                parser.parseCommandLine("cmd \"a b\" 'c d' e")
        );
    }

    /**
     * Verifies that within double quotes, a backslash escapes the next character, and the backslash
     * itself is not included in the resulting token.
     */
    @Test
    @DisplayName("Inside quotes, backslash escapes next character (double quotes)")
    void escapesInsideQuotes_doubleQuotes() {
        assertEquals(
                List.of("cmd", "a\"b", "tail"),
                parser.parseCommandLine("cmd \"a\\\"b\" tail")
        );
    }

    /**
     * Verifies that within single quotes, a backslash escapes the next character, and the backslash
     * itself is not included in the resulting token.
     */
    @Test
    @DisplayName("Inside quotes, backslash escapes next character (single quotes)")
    void escapesInsideQuotes_singleQuotes() {
        assertEquals(
                List.of("cmd", "a'b", "tail"),
                parser.parseCommandLine("cmd 'a\\'b' tail")
        );
    }

    /**
     * Verifies that outside quotes, backslash is treated as a regular character (no escape behavior).
     */
    @Test
    @DisplayName("Backslash outside quotes is treated as a normal character")
    void backslashOutsideQuotes_isLiteral() {
        assertEquals(
                List.of("cmd", "a\\b", "tail"),
                parser.parseCommandLine("cmd a\\b tail")
        );
    }

    /**
     * Verifies that an unclosed double quote causes {@link IllegalArgumentException}.
     */
    @Test
    @DisplayName("Throws for unclosed quote (double quote)")
    void throwsForUnclosedQuote_double() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseCommandLine("cmd \"oops")
        );
        assertTrue(ex.getMessage().contains("Unclosed quote"));
    }

    /**
     * Verifies that an unclosed single quote causes {@link IllegalArgumentException}.
     */
    @Test
    @DisplayName("Throws for unclosed quote (single quote)")
    void throwsForUnclosedQuote_single() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseCommandLine("cmd 'oops")
        );
        assertTrue(ex.getMessage().contains("Unclosed quote"));
    }

    /**
     * Verifies that leading/trailing whitespace and repeated whitespace do not produce empty tokens.
     */
    @Test
    @DisplayName("Does not create empty tokens for repeated spaces")
    void doesNotCreateEmptyTokens() {
        assertEquals(
                List.of("a", "b"),
                parser.parseCommandLine("  a    b   ")
        );
    }

    /**
     * Verifies that a trailing escape inside quotes without a following character keeps quotes unclosed
     * and results in {@link IllegalArgumentException}.
     */
    @Test
    @DisplayName("Throws for trailing escape inside quotes (no closing quote)")
    void throwsForTrailingEscapeInsideQuotes() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseCommandLine("cmd \"abc\\\"")
        );
        assertTrue(ex.getMessage().contains("Unclosed quote"));
    }

}
