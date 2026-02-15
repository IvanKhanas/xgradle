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
import org.altlinux.xgradle.interfaces.installers.JavadocInstaller;
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
@DisplayName("XmvnCompatController (javadoc)")
class JavadocXmvnCompatControllerTests {

    private XmvnCompatController controller;

    @Mock
    private JavadocInstaller javadocInstaller;

    @Mock
    private XmvnCompatController libraryXmvnController;

    @Mock
    private XmvnCompatController bomXmvnController;

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
                                bind(JavadocInstaller.class).toInstance(javadocInstaller);

                                bind(XmvnCompatController.class).annotatedWith(Library.class).toInstance(libraryXmvnController);
                                bind(XmvnCompatController.class).annotatedWith(Bom.class).toInstance(bomXmvnController);

                                bind(ArtifactsInstallationController.class).toInstance(pluginsController);
                                bind(PomRedactionController.class).toInstance(pomRedactionController);
                            }
                        })
        );

        controller = injector.getInstance(Key.get(XmvnCompatController.class, Javadoc.class));
    }

    @Test
    @DisplayName("Guice creates controller")
    void controllerIsCreated() {
        assertNotNull(controller);
    }

    @Test
    @DisplayName("Does nothing when javadoc flag is not set")
    void doesNothingWhenJavadocFlagIsNotSet() {
        when(args.hasJavadocRegistration()).thenReturn(false);

        controller.configureXmvnCompatFunctions(jCommander, new String[0], args, logger);

        verifyNoInteractions(javadocInstaller);
        verifyNoInteractions(jCommander);
    }

    @Test
    @DisplayName("Missing searching directory => throws CliUsageException and does not call installer")
    void throwsCliUsageExceptionWhenMissingSearchingDirectory() {
        when(args.hasJavadocRegistration()).thenReturn(true);
        when(args.hasSearchingDirectory()).thenReturn(false);

        assertThrows(CliUsageException.class,
                () -> controller.configureXmvnCompatFunctions(jCommander, new String[]{"--register-javadoc"}, args, logger));

        verifyNoInteractions(javadocInstaller);
        verifyNoInteractions(jCommander);
    }

    @Test
    @DisplayName("Missing JAR installation directory => throws CliUsageException and does not call installer")
    void throwsCliUsageExceptionWhenMissingJarInstallationDirectory() {
        when(args.hasJavadocRegistration()).thenReturn(true);
        when(args.hasSearchingDirectory()).thenReturn(true);
        when(args.hasJarInstallationDirectory()).thenReturn(false);

        assertThrows(CliUsageException.class,
                () -> controller.configureXmvnCompatFunctions(jCommander, new String[]{"--register-javadoc"}, args, logger));

        verifyNoInteractions(javadocInstaller);
        verifyNoInteractions(jCommander);
    }

    @Test
    @DisplayName("Delegates to JavadocInstaller when javadoc flag and required paths are set")
    void delegatesToJavadocInstaller() {
        Optional<List<String>> artifactNames = Optional.of(List.of("lib"));

        when(args.hasJavadocRegistration()).thenReturn(true);
        when(args.hasSearchingDirectory()).thenReturn(true);
        when(args.hasJarInstallationDirectory()).thenReturn(true);

        when(args.getSearchingDirectory()).thenReturn("/repo");
        when(args.getArtifactName()).thenReturn(artifactNames);
        when(args.getJarInstallationDirectory()).thenReturn("/target/javadoc");

        controller.configureXmvnCompatFunctions(jCommander, new String[]{"--register-javadoc"}, args, logger);

        verify(javadocInstaller).installJavadoc("/repo", artifactNames, "/target/javadoc");
        verifyNoInteractions(jCommander);
    }
}
