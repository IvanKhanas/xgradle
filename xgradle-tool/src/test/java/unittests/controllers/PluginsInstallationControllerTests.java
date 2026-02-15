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
import com.google.inject.util.Modules;

import org.altlinux.xgradle.interfaces.controllers.ArtifactsInstallationController;
import org.altlinux.xgradle.interfaces.installers.ArtifactsInstaller;
import org.altlinux.xgradle.interfaces.installers.JavadocInstaller;
import org.altlinux.xgradle.interfaces.registrars.Registrar;
import org.altlinux.xgradle.interfaces.services.PomService;

import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Bom;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Library;
import org.altlinux.xgradle.impl.cli.CliArgumentsContainer;
import org.altlinux.xgradle.impl.config.ToolConfig;
import org.altlinux.xgradle.impl.controllers.ControllersModule;
import org.altlinux.xgradle.impl.enums.ProcessingType;
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
@DisplayName("ArtifactsInstallationController contract")
class PluginsInstallationControllerTests {

    private ArtifactsInstallationController controller;

    @Mock
    private ArtifactsInstaller artifactsInstaller;

    @Mock
    private CliArgumentsContainer args;

    @Mock
    private PomService pomService;

    @Mock
    private JavadocInstaller javadocInstaller;

    @Mock
    private Registrar libraryRegistrar;

    @Mock
    private Registrar bomRegistrar;

    @Mock
    private Logger logger;

    private final JCommander jc = JCommander.newBuilder().build();

    @BeforeEach
    void setUp() {
        ToolConfig toolConfig = new ToolConfig(args);

        Injector injector = Guice.createInjector(
                Modules.override(new ControllersModule())
                        .with(new AbstractModule() {
                            @Override
                            protected void configure() {
                                bind(ArtifactsInstaller.class).toInstance(artifactsInstaller);
                                bind(CliArgumentsContainer.class).toInstance(args);
                                bind(ToolConfig.class).toInstance(toolConfig);

                                bind(PomService.class).toInstance(pomService);
                                bind(JavadocInstaller.class).toInstance(javadocInstaller);

                                bind(Registrar.class).annotatedWith(Library.class).toInstance(libraryRegistrar);
                                bind(Registrar.class).annotatedWith(Bom.class).toInstance(bomRegistrar);
                            }
                        })
        );

        controller = injector.getInstance(ArtifactsInstallationController.class);
    }

    @Test
    @DisplayName("Guice creates controller")
    void createdByGuice() {
        assertNotNull(controller);
    }

    @Test
    @DisplayName("Does nothing when install-plugin parameter is not set")
    void doesNothingWhenInstallPluginNotSet() {
        when(args.hasInstallPluginParameter()).thenReturn(false);

        controller.configurePluginArtifactsInstallation(jc, new String[0], args, logger);

        verifyNoInteractions(artifactsInstaller);
    }

    @Test
    @DisplayName("Missing searching directory => throws CliUsageException and does not call installer")
    void throwsCliUsageExceptionWhenSearchingDirectoryMissing() {
        when(args.hasInstallPluginParameter()).thenReturn(true);
        when(args.hasSearchingDirectory()).thenReturn(false);

        assertThrows(CliUsageException.class,
                () -> controller.configurePluginArtifactsInstallation(jc, new String[0], args, logger));

        verifyNoInteractions(artifactsInstaller);
    }

    @Test
    @DisplayName("Missing artifact name => throws CliUsageException and does not call installer")
    void throwsCliUsageExceptionWhenArtifactNameMissing() {
        when(args.hasInstallPluginParameter()).thenReturn(true);
        when(args.hasSearchingDirectory()).thenReturn(true);
        when(args.hasArtifactName()).thenReturn(false);

        assertThrows(CliUsageException.class,
                () -> controller.configurePluginArtifactsInstallation(jc, new String[0], args, logger));

        verifyNoInteractions(artifactsInstaller);
    }

    @Test
    @DisplayName("Missing POM installation directory => throws CliUsageException and does not call installer")
    void throwsCliUsageExceptionWhenPomDirMissing() {
        when(args.hasInstallPluginParameter()).thenReturn(true);
        when(args.hasSearchingDirectory()).thenReturn(true);
        when(args.hasArtifactName()).thenReturn(true);
        when(args.hasPomInstallationDirectory()).thenReturn(false);

        assertThrows(CliUsageException.class,
                () -> controller.configurePluginArtifactsInstallation(jc, new String[0], args, logger));

        verifyNoInteractions(artifactsInstaller);
    }

    @Test
    @DisplayName("Missing JAR installation directory => throws CliUsageException and does not call installer")
    void throwsCliUsageExceptionWhenJarDirMissing() {
        when(args.hasInstallPluginParameter()).thenReturn(true);
        when(args.hasSearchingDirectory()).thenReturn(true);
        when(args.hasArtifactName()).thenReturn(true);
        when(args.hasPomInstallationDirectory()).thenReturn(true);
        when(args.hasJarInstallationDirectory()).thenReturn(false);

        assertThrows(CliUsageException.class,
                () -> controller.configurePluginArtifactsInstallation(jc, new String[0], args, logger));

        verifyNoInteractions(artifactsInstaller);
    }

    @Test
    @DisplayName("configurePluginArtifactsInstallation: delegates to ArtifactsInstaller.install")
    void delegatesToArtifactsInstallerInstall() {
        when(args.hasInstallPluginParameter()).thenReturn(true);

        when(args.hasSearchingDirectory()).thenReturn(true);
        when(args.getSearchingDirectory()).thenReturn("/repo");

        when(args.hasArtifactName()).thenReturn(true);
        when(args.getArtifactName()).thenReturn(Optional.of(List.of("my-plugin")));

        when(args.hasPomInstallationDirectory()).thenReturn(true);
        when(args.getPomInstallationDirectory()).thenReturn("/poms");

        when(args.hasJarInstallationDirectory()).thenReturn(true);
        when(args.getJarInstallationDirectory()).thenReturn("/jars");

        controller.configurePluginArtifactsInstallation(jc, new String[0], args, logger);

        verify(artifactsInstaller).install(
                "/repo",
                Optional.of(List.of("my-plugin")),
                "/poms",
                "/jars",
                ProcessingType.PLUGINS
        );
        verifyNoMoreInteractions(artifactsInstaller);
    }
}
