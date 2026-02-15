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
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Library;
import org.altlinux.xgradle.impl.enums.ExitCode;
import org.altlinux.xgradle.impl.enums.ProcessingType;
import org.altlinux.xgradle.impl.exceptions.EmptyRegisterCommandException;
import org.altlinux.xgradle.impl.registrars.RegistrarsModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class XmvnCompatRegistrarTests {

    private static final String DIRECTORY = "/repo";
    private static final String CMD = "/usr/bin/python3 /usr/share/java-utils/mvn_artifact.py";
    private static final List<String> PARSED_CMD = List.of("/usr/bin/python3 /usr/share/java-utils/mvn_artifact.py");

    @Mock
    private ArtifactContainer artifactContainer;

    @Mock
    private CommandExecutor commandExecutor;

    @Mock
    private CommandLineParser commandLineParser;

    @Mock
    private Logger logger;

    @Mock
    private PomProcessor<Set<Path>> bomPomProcessorDummy;

    private Registrar registrar;

    @BeforeEach
    void setUp() {
        Injector injector = Guice.createInjector(
                Modules.override(new RegistrarsModule())
                        .with(new AbstractModule() {
                            @Override
                            protected void configure() {
                                bind(ArtifactContainer.class).toInstance(artifactContainer);
                                bind(CommandExecutor.class).toInstance(commandExecutor);
                                bind(CommandLineParser.class).toInstance(commandLineParser);
                                bind(Logger.class).toInstance(logger);

                                bind(Key.get(new TypeLiteral<PomProcessor<Set<Path>>>() {}, Bom.class))
                                        .toInstance(bomPomProcessorDummy);
                            }
                        })
        );

        registrar = injector.getInstance(Key.get(Registrar.class, Library.class));
    }

    @Test
    @DisplayName("artifactName absent: gets artifacts with Optional.empty and registers each pom+jar pair")
    void registerAllPairsWithoutNames() throws Exception {

        Map<String, Path> artifacts = new LinkedHashMap<>();
        artifacts.put("/repo/a.pom", Path.of("/repo/a.jar"));
        artifacts.put("/repo/b.pom", Path.of("/repo/b.jar"));

        when(artifactContainer.getArtifacts(eq(DIRECTORY), eq(Optional.empty()), eq(ProcessingType.LIBRARY)))
                .thenReturn(new LinkedHashMap<>(artifacts));

        when(commandLineParser.parseCommandLine(CMD))
                .thenReturn(PARSED_CMD);

        when(commandExecutor.execute(any(ProcessBuilder.class)))
                .thenReturn(ExitCode.SUCCESS.getExitCode());

        registrar.registerArtifacts(DIRECTORY, CMD, Optional.empty());

        verify(artifactContainer).getArtifacts(DIRECTORY, Optional.empty(), ProcessingType.LIBRARY);

        ArgumentCaptor<ProcessBuilder> processBuilderCaptor = ArgumentCaptor.forClass(ProcessBuilder.class);
        verify(commandExecutor, times(2)).execute(processBuilderCaptor.capture());

        List<ProcessBuilder> pbs = processBuilderCaptor.getAllValues();

        assertEquals(List.of(PARSED_CMD.get(0), "/repo/a.pom", "/repo/a.jar"), pbs.get(0).command());
        assertEquals(List.of(PARSED_CMD.get(0), "/repo/b.pom", "/repo/b.jar"), pbs.get(1).command());

        verify(logger).info("Artifacts registered successfully");
    }

    @Test
    @DisplayName("artifactName present: passes names filter to container")
    void userNamesFilter() throws Exception {
        Optional<List<String>> names = Optional.of(List.of("x", "y"));

        Map<String, Path> artifacts = new LinkedHashMap<>();
        artifacts.put("repo/x.pom", Path.of("/repo/x.jar"));

        when(artifactContainer.getArtifacts(eq(DIRECTORY), eq(names), eq(ProcessingType.LIBRARY)))
                .thenReturn(new LinkedHashMap<>(artifacts));

        when(commandLineParser.parseCommandLine(CMD)).thenReturn(PARSED_CMD);
        when(commandExecutor.execute(any(ProcessBuilder.class))).thenReturn(ExitCode.SUCCESS.getExitCode());

        registrar.registerArtifacts(DIRECTORY, CMD, names);

        verify(artifactContainer).getArtifacts(DIRECTORY, names, ProcessingType.LIBRARY);
        verify(commandExecutor, times(1)).execute(any(ProcessBuilder.class));
        verify(logger).info("Artifacts registered successfully");
    }

    @Test
    @DisplayName("No artifacts -> logs and does not execute")
    void logsWhenNoArtifacts() throws IOException, InterruptedException {
        when(artifactContainer.getArtifacts(eq(DIRECTORY), eq(Optional.empty()), eq(ProcessingType.LIBRARY)))
                .thenReturn(new LinkedHashMap<>());

        when(commandLineParser.parseCommandLine(CMD)).thenReturn(PARSED_CMD);

        registrar.registerArtifacts(DIRECTORY, CMD, Optional.empty());

        InOrder inOrder = inOrder(commandLineParser, logger);
        inOrder.verify(commandLineParser).parseCommandLine(CMD);
        inOrder.verify(logger).info("No artifacts registered");

        verify(commandExecutor, never()).execute(any(ProcessBuilder.class));
    }

    @Test
    @DisplayName("Empty parsed command -> throws and does not execute")
    void throwsWhenCommandEmpty() throws IOException, InterruptedException {

        when(artifactContainer.getArtifacts(eq(DIRECTORY), eq(Optional.empty()), eq(ProcessingType.LIBRARY)))
                .thenReturn(new LinkedHashMap<>());

        when(commandLineParser.parseCommandLine("")).thenReturn(List.of());

        EmptyRegisterCommandException exception = assertThrows(
                EmptyRegisterCommandException.class,
                () -> registrar.registerArtifacts(DIRECTORY, "", Optional.empty())
        );
                assertEquals("Register command is empty: " + "\'\'", exception.getMessage());
                verify(commandExecutor, never()).execute(any());
    }

    @Test
    @DisplayName("Non-zero exit -> throws RuntimeException")
    void throwsWhenExitCodeNotSuccess() throws IOException, InterruptedException {
        Map<String, Path> artifacts = new LinkedHashMap<>();
        artifacts.put("/repo/a.pom", Path.of("/repo/a.jar"));

        when(artifactContainer.getArtifacts(eq(DIRECTORY), eq(Optional.empty()), eq(ProcessingType.LIBRARY)))
                .thenReturn(new LinkedHashMap<>(artifacts));

        when(commandLineParser.parseCommandLine(CMD)).thenReturn(PARSED_CMD);

        when(commandExecutor.execute(any(ProcessBuilder.class)))
                .thenReturn(ExitCode.ERROR.getExitCode());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> registrar.registerArtifacts(DIRECTORY, CMD, Optional.empty())
        );

        assertTrue(exception.getMessage().contains("Failed to register artifact, exit code:"));
    }
}
