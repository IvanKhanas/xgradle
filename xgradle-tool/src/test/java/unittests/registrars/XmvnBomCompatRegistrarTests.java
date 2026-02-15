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
package unittests.registrars;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;

import org.altlinux.xgradle.interfaces.cli.CommandExecutor;
import org.altlinux.xgradle.interfaces.cli.CommandLineParser;
import org.altlinux.xgradle.interfaces.containers.ArtifactContainer;
import org.altlinux.xgradle.interfaces.processors.PomProcessor;
import org.altlinux.xgradle.interfaces.registrars.Registrar;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Bom;
import org.altlinux.xgradle.impl.enums.ExitCode;
import org.altlinux.xgradle.impl.exceptions.EmptyRegisterCommandException;
import org.altlinux.xgradle.impl.registrars.RegistrarsModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("@Bom Registrar (XmvnBomCompatRegistrar)")
class XmvnBomCompatRegistrarTests {

    private static final String DIRECTORY = "/repo";
    private static final String CMD = "/usr/bin/python3 /usr/share/java-utils/mvn_artifact.py";
    private static final List<String> PARSED_CMD = List.of("/usr/bin/python3 /usr/share/java-utils/mvn_artifact.py");

    @Mock
    private PomProcessor<Set<Path>> bomPomProcessor;

    @Mock
    private CommandExecutor commandExecutor;

    @Mock
    private CommandLineParser commandLineParser;

    @Mock
    private Logger logger;

    @Mock
    private ArtifactContainer artifactContainerDummy;

    private Registrar registrar;

    @BeforeEach
    void setUp() {
        Injector injector = Guice.createInjector(
                Modules.override(new RegistrarsModule())
                        .with(new AbstractModule() {
                            @Override
                            protected void configure() {
                                bind(Key.get(new TypeLiteral<PomProcessor<Set<Path>>>() {}, Bom.class))
                                        .toInstance(bomPomProcessor);

                                bind(CommandExecutor.class).toInstance(commandExecutor);
                                bind(CommandLineParser.class).toInstance(commandLineParser);
                                bind(Logger.class).toInstance(logger);

                                bind(ArtifactContainer.class).toInstance(artifactContainerDummy);
                            }
                        })
        );

        registrar = injector.getInstance(Key.get(Registrar.class, Bom.class));
    }

    @Test
    @DisplayName("artifactName absent: registers each BOM path")
    void registersBomsWithoutNames() throws Exception {
        Set<Path> boms = new LinkedHashSet<>();
        boms.add(Path.of("/repo/bom-1.pom"));
        boms.add(Path.of("/repo/bom-2.pom"));

        when(bomPomProcessor.pomsFromDirectory(eq(DIRECTORY), eq(Optional.empty()))).thenReturn(boms);
        when(commandLineParser.parseCommandLine(CMD)).thenReturn(PARSED_CMD);
        when(commandExecutor.execute(any(ProcessBuilder.class))).thenReturn(ExitCode.SUCCESS.getExitCode());

        registrar.registerArtifacts(DIRECTORY, CMD, Optional.empty());

        verify(bomPomProcessor).pomsFromDirectory(DIRECTORY, Optional.empty());

        ArgumentCaptor<ProcessBuilder> cap = ArgumentCaptor.forClass(ProcessBuilder.class);
        verify(commandExecutor, times(2)).execute(cap.capture());

        List<ProcessBuilder> pbs = cap.getAllValues();
        assertEquals(List.of(PARSED_CMD.get(0), "/repo/bom-1.pom"), pbs.get(0).command());
        assertEquals(List.of(PARSED_CMD.get(0), "/repo/bom-2.pom"), pbs.get(1).command());

        verify(logger).info("BOM`s registered successfully");
    }

    @Test
    @DisplayName("artifactName present: passes names to processor")
    void usesNamesFilter() throws Exception {
        Optional<List<String>> names = Optional.of(List.of("a", "b"));

        Set<Path> boms = new LinkedHashSet<>();
        boms.add(Path.of("/repo/bom-a.pom"));

        when(bomPomProcessor.pomsFromDirectory(eq(DIRECTORY), eq(names))).thenReturn(boms);
        when(commandLineParser.parseCommandLine(CMD)).thenReturn(PARSED_CMD);
        when(commandExecutor.execute(any(ProcessBuilder.class))).thenReturn(ExitCode.SUCCESS.getExitCode());

        registrar.registerArtifacts(DIRECTORY, CMD, names);

        verify(bomPomProcessor).pomsFromDirectory(DIRECTORY, names);
        verify(commandExecutor, times(1)).execute(any(ProcessBuilder.class));
        verify(logger).info("BOM`s registered successfully");
    }

    @Test
    @DisplayName("No BOMs => logs and does not execute")
    void logsWhenNoBoms() throws IOException, InterruptedException {
        when(bomPomProcessor.pomsFromDirectory(eq(DIRECTORY), eq(Optional.empty()))).thenReturn(Set.of());
        when(commandLineParser.parseCommandLine(CMD)).thenReturn(PARSED_CMD);

        registrar.registerArtifacts(DIRECTORY, CMD, Optional.empty());

        verify(commandExecutor, never()).execute(any());
        verify(logger).info("No BOM registered");
    }

    @Test
    @DisplayName("Empty parsed command (when there is at least one BOM) => throws and does not execute")
    void throwsWhenCommandEmpty() throws IOException, InterruptedException {
        Set<Path> boms = new LinkedHashSet<>();
        boms.add(Path.of("/repo/bom-1.pom"));

        when(bomPomProcessor.pomsFromDirectory(eq(DIRECTORY), eq(Optional.empty()))).thenReturn(boms);
        when(commandLineParser.parseCommandLine("   ")).thenReturn(List.of());

        EmptyRegisterCommandException ex = assertThrows(
                EmptyRegisterCommandException.class,
                () -> registrar.registerArtifacts(DIRECTORY, "   ", Optional.empty())
        );
        assertEquals("Register command is empty: " + "\'   \'", ex.getMessage());

        verify(commandExecutor, never()).execute(any());
    }

    @Test
    @DisplayName("Non-zero exit => throws RuntimeException")
    void throwsWhenExitCodeNotSuccess() throws Exception {
        Set<Path> boms = new LinkedHashSet<>();
        boms.add(Path.of("/repo/bom-1.pom"));

        when(bomPomProcessor.pomsFromDirectory(eq(DIRECTORY), eq(Optional.empty()))).thenReturn(boms);
        when(commandLineParser.parseCommandLine(CMD)).thenReturn(PARSED_CMD);
        when(commandExecutor.execute(any(ProcessBuilder.class))).thenReturn(ExitCode.ERROR.getExitCode());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> registrar.registerArtifacts(DIRECTORY, CMD, Optional.empty())
        );

        assertTrue(ex.getMessage().contains("Failed to register artifact, exit code:"));
    }

    @Test
    @DisplayName("IOException => wraps into RuntimeException (cause preserved)")
    void wrapsIOException() throws Exception {
        Set<Path> boms = new LinkedHashSet<>();
        boms.add(Path.of("/repo/bom-1.pom"));

        when(bomPomProcessor.pomsFromDirectory(eq(DIRECTORY), eq(Optional.empty()))).thenReturn(boms);
        when(commandLineParser.parseCommandLine(CMD)).thenReturn(PARSED_CMD);
        when(commandExecutor.execute(any(ProcessBuilder.class))).thenThrow(new IOException("boom"));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> registrar.registerArtifacts(DIRECTORY, CMD, Optional.empty())
        );

        assertNotNull(ex.getCause());
        assertEquals(IOException.class, ex.getCause().getClass());
    }

    @Test
    @DisplayName("InterruptedException => wraps into RuntimeException (cause preserved)")
    void wrapsInterruptedException() throws Exception {
        Set<Path> boms = new LinkedHashSet<>();
        boms.add(Path.of("/repo/bom-1.pom"));

        when(bomPomProcessor.pomsFromDirectory(eq(DIRECTORY), eq(Optional.empty()))).thenReturn(boms);
        when(commandLineParser.parseCommandLine(CMD)).thenReturn(PARSED_CMD);
        when(commandExecutor.execute(any(ProcessBuilder.class))).thenThrow(new InterruptedException("stop"));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> registrar.registerArtifacts(DIRECTORY, CMD, Optional.empty())
        );

        assertNotNull(ex.getCause());
        assertEquals(InterruptedException.class, ex.getCause().getClass());
    }
}
