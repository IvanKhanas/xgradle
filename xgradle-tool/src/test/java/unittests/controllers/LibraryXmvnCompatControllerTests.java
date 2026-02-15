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

import org.altlinux.xgradle.interfaces.controllers.XmvnCompatController;
import org.altlinux.xgradle.interfaces.installers.ArtifactsInstaller;
import org.altlinux.xgradle.interfaces.installers.JavadocInstaller;
import org.altlinux.xgradle.interfaces.registrars.Registrar;
import org.altlinux.xgradle.interfaces.services.PomService;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Bom;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Library;
import org.altlinux.xgradle.impl.cli.CliArgumentsContainer;
import org.altlinux.xgradle.impl.config.ToolConfig;
import org.altlinux.xgradle.impl.controllers.ControllersModule;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("XmvnCompatController (library)")
class LibraryXmvnCompatControllerTests {

    private XmvnCompatController controller;

    @Mock private Registrar libraryRegistrar;
    @Mock private Registrar bomRegistrar;
    @Mock private JavadocInstaller javadocInstaller;
    @Mock private ArtifactsInstaller artifactsInstaller;
    @Mock private PomService pomService;

    @Mock private CliArgumentsContainer args;
    @Mock private Logger logger;

    private JCommander jc;

    @BeforeEach
    void setUp() {
        ToolConfig toolConfig = new ToolConfig(args);

        Injector injector = Guice.createInjector(
                Modules.override(new ControllersModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(new Key<Registrar>(Library.class) {}).toInstance(libraryRegistrar);
                        bind(new Key<Registrar>(Bom.class) {}).toInstance(bomRegistrar);

                        bind(JavadocInstaller.class).toInstance(javadocInstaller);
                        bind(ArtifactsInstaller.class).toInstance(artifactsInstaller);

                        bind(PomService.class).toInstance(pomService);
                        bind(ToolConfig.class).toInstance(toolConfig);
                    }
                })
        );

        controller = injector.getInstance(Key.get(XmvnCompatController.class, Library.class));
        jc = JCommander.newBuilder().build();
    }

    @Test
    @DisplayName("Guice creates controller")
    void createdByGuice() {
        assertNotNull(controller);
    }

    @Test
    @DisplayName("Does nothing when xmvn-register is not set")
    void doesNothingWhenXmvnRegisterNotSet() {
        when(args.hasXmvnRegister()).thenReturn(false);

        controller.configureXmvnCompatFunctions(jc, new String[0], args, logger);

        verifyNoInteractions(libraryRegistrar);
    }

    @Test
    @DisplayName("Delegates to @Library Registrar when xmvn-register is set and bom/javadoc are not set")
    void delegatesWhenXmvnRegisterSetAndNotBomNorJavadoc() {
        Optional<List<String>> names = Optional.of(List.of("a"));

        when(args.hasXmvnRegister()).thenReturn(true);
        when(args.hasBomRegistration()).thenReturn(false);
        when(args.hasJavadocRegistration()).thenReturn(false);

        when(args.hasSearchingDirectory()).thenReturn(true);
        when(args.getSearchingDirectory()).thenReturn("repo");

        when(args.getXmvnRegister()).thenReturn("cmd");
        when(args.getArtifactName()).thenReturn(names);

        controller.configureXmvnCompatFunctions(jc, new String[0], args, logger);

        verify(libraryRegistrar).registerArtifacts("repo", "cmd", names);
        verifyNoMoreInteractions(libraryRegistrar);
    }

    @Test
    @DisplayName("Does nothing when bom flag is set (library controller must not handle it)")
    void doesNothingWhenBomFlagIsSet() {
        when(args.hasXmvnRegister()).thenReturn(true);
        when(args.hasBomRegistration()).thenReturn(true);

        controller.configureXmvnCompatFunctions(jc, new String[0], args, logger);

        verifyNoInteractions(libraryRegistrar);
    }
}
