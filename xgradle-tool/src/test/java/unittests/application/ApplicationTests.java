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
package unittests.application;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

import org.altlinux.xgradle.interfaces.application.Application;
import org.altlinux.xgradle.interfaces.controllers.ArtifactsInstallationController;
import org.altlinux.xgradle.interfaces.controllers.PomRedactionController;
import org.altlinux.xgradle.interfaces.controllers.XmvnCompatController;
import org.altlinux.xgradle.impl.application.ApplicationModule;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Bom;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Javadoc;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Library;
import org.altlinux.xgradle.impl.cli.CliArgumentsContainer;
import org.altlinux.xgradle.impl.enums.ExitCode;
import org.altlinux.xgradle.impl.exceptions.CliUsageException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Application contract")
class ApplicationTests {

    @Mock
    private JCommander jCommander;

    @Mock
    private CliArgumentsContainer cliArgs;

    @Mock
    private Logger logger;

    @Mock
    private XmvnCompatController libraryXmvnController;

    @Mock
    private XmvnCompatController bomXmvnController;

    @Mock
    private XmvnCompatController javadocXmvnController;

    @Mock
    private ArtifactsInstallationController pluginsController;

    @Mock
    private PomRedactionController pomRedactionController;

    private Application application;

    @BeforeEach
    void setUp() {
        Injector injector = Guice.createInjector(
                Modules.override(new ApplicationModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(JCommander.class).toInstance(jCommander);
                        bind(CliArgumentsContainer.class).toInstance(cliArgs);
                        bind(Logger.class).toInstance(logger);

                        bind(XmvnCompatController.class).annotatedWith(Library.class).toInstance(libraryXmvnController);
                        bind(XmvnCompatController.class).annotatedWith(Bom.class).toInstance(bomXmvnController);
                        bind(XmvnCompatController.class).annotatedWith(Javadoc.class).toInstance(javadocXmvnController);

                        bind(ArtifactsInstallationController.class).toInstance(pluginsController);
                        bind(PomRedactionController.class).toInstance(pomRedactionController);
                    }
                })
        );

        application = injector.getInstance(Application.class);
    }

    @Test
    @DisplayName("Is created by Guice")
    void isCreatedByGuice() {
        assertNotNull(application);
    }

    @Test
    @DisplayName("run: empty args prints usage and returns SUCCESS")
    void emptyArgsPrintsUsageAndReturnsSuccess() {
        ExitCode code = application.run(new String[0]);

        assertEquals(ExitCode.SUCCESS, code);
        verify(jCommander).usage();

        verifyNoInteractions(libraryXmvnController, bomXmvnController, javadocXmvnController, pluginsController, pomRedactionController);
    }

    @Test
    @DisplayName("run: --help prints usage and returns SUCCESS")
    void helpPrintsUsageAndReturnsSuccess() {
        when(cliArgs.hasHelp()).thenReturn(true);

        ExitCode code = application.run(new String[]{"--help"});

        assertEquals(ExitCode.SUCCESS, code);
        verify(jCommander).usage();

        verifyNoInteractions(libraryXmvnController, bomXmvnController, javadocXmvnController, pluginsController, pomRedactionController);
    }

    @Test
    @DisplayName("run: validateMutuallyExclusive error logs, prints usage and returns ERROR")
    void validateMutuallyExclusiveErrorLogsPrintsUsageAndReturnsError() {
        doThrow(new ParameterException("bad args")).when(cliArgs).validateMutuallyExclusive();

        ExitCode code = application.run(new String[]{"--xmvn-register=cmd"});

        assertEquals(ExitCode.ERROR, code);
        verify(logger).error("bad args");
        verify(jCommander).usage();

        verifyNoInteractions(libraryXmvnController, bomXmvnController, javadocXmvnController, pluginsController, pomRedactionController);
    }

    @Test
    @DisplayName("run: parse error logs, prints usage and returns ERROR")
    void parseErrorLogsPrintsUsageAndReturnsError() {
        doThrow(new ParameterException("bad args")).when(jCommander).parse(any(String[].class));

        ExitCode code = application.run(new String[]{"--xmvn-register=cmd"});

        assertEquals(ExitCode.ERROR, code);
        verify(logger).error("bad args");
        verify(jCommander).usage();

        verifyNoInteractions(libraryXmvnController, bomXmvnController, javadocXmvnController, pluginsController, pomRedactionController);
    }

    @Test
    @DisplayName("run: CliUsageException logs message, prints usage and returns ERROR")
    void cliUsageExceptionLogsPrintsUsageAndReturnsError() {
        doThrow(new CliUsageException("missing required arg"))
                .when(libraryXmvnController).configureXmvnCompatFunctions(any(), any(), any(), any());

        ExitCode code = application.run(new String[]{"--xmvn-register=cmd"});

        assertEquals(ExitCode.ERROR, code);
        verify(logger).error(eq("missing required arg"));
        verify(jCommander).usage();

        verifyNoInteractions(pluginsController, bomXmvnController, javadocXmvnController, pomRedactionController);
    }

    @Test
    @DisplayName("run: controller exception logs and returns ERROR")
    void controllerExceptionLogsAndReturnsError() {
        RuntimeException boom = new RuntimeException("boom");
        doThrow(boom).when(libraryXmvnController).configureXmvnCompatFunctions(any(), any(), any(), any());

        ExitCode code = application.run(new String[]{"--xmvn-register=cmd"});

        assertEquals(ExitCode.ERROR, code);
        verify(logger).error(eq("boom"), same(boom));

        verifyNoInteractions(pluginsController, bomXmvnController, javadocXmvnController, pomRedactionController);
    }

    @Test
    @DisplayName("run: normal flow calls all controllers once and returns SUCCESS")
    void normalFlowCallsAllControllersOnceAndReturnsSuccess() {
        String[] args = {"--xmvn-register=cmd"};

        ExitCode code = application.run(args);

        assertEquals(ExitCode.SUCCESS, code);

        verify(libraryXmvnController).configureXmvnCompatFunctions(same(jCommander), same(args), same(cliArgs), same(logger));
        verify(pluginsController).configurePluginArtifactsInstallation(same(jCommander), same(args), same(cliArgs), same(logger));
        verify(bomXmvnController).configureXmvnCompatFunctions(same(jCommander), same(args), same(cliArgs), same(logger));
        verify(javadocXmvnController).configureXmvnCompatFunctions(same(jCommander), same(args), same(cliArgs), same(logger));

        verifyNoInteractions(pomRedactionController);
    }
}
