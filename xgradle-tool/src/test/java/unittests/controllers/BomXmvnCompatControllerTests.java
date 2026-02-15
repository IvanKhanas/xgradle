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
package unittests.controllers;

import com.beust.jcommander.JCommander;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.util.Modules;

import org.altlinux.xgradle.interfaces.controllers.ArtifactsInstallationController;
import org.altlinux.xgradle.interfaces.controllers.PomRedactionController;
import org.altlinux.xgradle.interfaces.controllers.XmvnCompatController;
import org.altlinux.xgradle.interfaces.registrars.Registrar;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Bom;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Javadoc;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Library;
import org.altlinux.xgradle.impl.cli.CliArgumentsContainer;
import org.altlinux.xgradle.impl.controllers.ControllersModule;
import org.altlinux.xgradle.impl.exceptions.CliUsageException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("XmvnCompatController (bom)")
class BomXmvnCompatControllerTests {

    private XmvnCompatController controller;

    @Mock
    private Registrar registrar;

    @Mock
    private XmvnCompatController libraryXmvnController;

    @Mock
    private XmvnCompatController javadocXmvnController;

    @Mock
    private ArtifactsInstallationController pluginsController;

    @Mock
    private PomRedactionController pomRedactionController;

    @Mock
    private JCommander jCommander;

    @Mock
    private CliArgumentsContainer args;

    @Mock
    private Logger logger;

    @BeforeEach
    void setUp() {
        Injector injector = Guice.createInjector(
                Modules.override(new ControllersModule())
                        .with(new AbstractModule() {
                            @Override
                            protected void configure() {
                                bind(Registrar.class).annotatedWith(Bom.class).toInstance(registrar);

                                bind(XmvnCompatController.class).annotatedWith(Library.class).toInstance(libraryXmvnController);
                                bind(XmvnCompatController.class).annotatedWith(Javadoc.class).toInstance(javadocXmvnController);

                                bind(ArtifactsInstallationController.class).toInstance(pluginsController);
                                bind(PomRedactionController.class).toInstance(pomRedactionController);
                            }
                        })
        );

        controller = injector.getInstance(Key.get(XmvnCompatController.class, Bom.class));
    }

    @Test
    @DisplayName("Guice creates controller")
    void controllerIsCreated() {
        assertNotNull(controller);
    }

    @Test
    @DisplayName("Does nothing when xmvn-register is not set")
    void doesNothingWhenXmvnRegisterIsNotSet() {
        when(args.hasXmvnRegister()).thenReturn(false);

        controller.configureXmvnCompatFunctions(jCommander, new String[0], args, logger);

        verifyNoInteractions(registrar);
        verifyNoInteractions(jCommander);
    }

    @Test
    @DisplayName("Does nothing when bom flag is not set (bom controller must not handle library registration)")
    void doesNothingWhenBomFlagIsNotSet() {
        when(args.hasXmvnRegister()).thenReturn(true);
        when(args.hasBomRegistration()).thenReturn(false);

        controller.configureXmvnCompatFunctions(jCommander, new String[0], args, logger);

        verifyNoInteractions(registrar);
        verifyNoInteractions(jCommander);
    }

    @Test
    @DisplayName("Missing searching directory => throws CliUsageException and does not call registrar")
    void throwsCliUsageExceptionWhenMissingSearchingDirectory() {
        when(args.hasXmvnRegister()).thenReturn(true);
        when(args.hasBomRegistration()).thenReturn(true);
        when(args.hasSearchingDirectory()).thenReturn(false);

        assertThrows(CliUsageException.class,
                () -> controller.configureXmvnCompatFunctions(jCommander, new String[]{"--register-bom"}, args, logger));

        verifyNoInteractions(registrar);
        verifyNoInteractions(jCommander);
    }

    @Test
    @DisplayName("Delegates to @Bom Registrar when xmvn-register is set and bom flag is set")
    void delegatesToBomRegistrar() {
        Optional<List<String>> artifactNames = Optional.of(List.of("a", "b"));

        when(args.hasXmvnRegister()).thenReturn(true);
        when(args.hasBomRegistration()).thenReturn(true);
        when(args.hasSearchingDirectory()).thenReturn(true);
        when(args.getSearchingDirectory()).thenReturn("/repo");
        when(args.getXmvnRegister()).thenReturn("/usr/bin/xmvn-register");
        when(args.getArtifactName()).thenReturn(artifactNames);

        controller.configureXmvnCompatFunctions(jCommander, new String[]{"--register-bom"}, args, logger);

        verify(registrar).registerArtifacts("/repo", "/usr/bin/xmvn-register", artifactNames);
        verifyNoInteractions(jCommander);
    }
}
