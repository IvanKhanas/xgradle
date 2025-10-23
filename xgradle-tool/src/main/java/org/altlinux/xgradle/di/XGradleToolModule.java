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
package org.altlinux.xgradle.di;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import org.altlinux.xgradle.ToolConfig;
import org.altlinux.xgradle.api.cli.CommandExecutor;
import org.altlinux.xgradle.api.cli.CommandLineParser;
import org.altlinux.xgradle.api.controllers.ArtifactsInstallationController;
import org.altlinux.xgradle.api.controllers.XmvnCompatController;
import org.altlinux.xgradle.api.installers.ArtifactsInstaller;
import org.altlinux.xgradle.api.installers.JavadocInstaller;
import org.altlinux.xgradle.api.model.ArtifactCache;
import org.altlinux.xgradle.api.parsers.PomParser;
import org.altlinux.xgradle.api.services.PomService;
import org.altlinux.xgradle.api.processors.PomProcessor;
import org.altlinux.xgradle.api.collectors.ArtifactCollector;
import org.altlinux.xgradle.api.collectors.PomCollector;
import org.altlinux.xgradle.api.containers.ArtifactContainer;
import org.altlinux.xgradle.api.containers.PomContainer;
import org.altlinux.xgradle.api.redactors.ParentRedactor;
import org.altlinux.xgradle.api.registrars.Registrar;
import org.altlinux.xgradle.cli.DefaultCommandExecutor;
import org.altlinux.xgradle.cli.DefaultCommandLineParser;
import org.altlinux.xgradle.collectors.DefaultArtifactCollector;
import org.altlinux.xgradle.collectors.DefaultPomCollector;
import org.altlinux.xgradle.containers.DefaultArtifactContainer;
import org.altlinux.xgradle.containers.DefaultPomContainer;
import org.altlinux.xgradle.controllers.DefaultBomXmvnCompatController;
import org.altlinux.xgradle.controllers.DefaultJavadocXmvnCompatController;
import org.altlinux.xgradle.controllers.DefaultPluginsInstallationController;
import org.altlinux.xgradle.controllers.DefaultXmvnCompatController;
import org.altlinux.xgradle.installers.DefaultJavadocInstaller;
import org.altlinux.xgradle.installers.DefaultPluginArtifactsInstaller;
import org.altlinux.xgradle.model.DefaultArtifactCache;
import org.altlinux.xgradle.parsers.ConcurrentBomParser;
import org.altlinux.xgradle.parsers.ConcurrentJavadocParser;
import org.altlinux.xgradle.parsers.ConcurrentLibraryPomParser;
import org.altlinux.xgradle.parsers.DefaultPluginPomParser;
import org.altlinux.xgradle.processors.DefaultBomProcessor;
import org.altlinux.xgradle.processors.DefaultJavadocProcessor;
import org.altlinux.xgradle.processors.DefaultLibraryPomProcessor;
import org.altlinux.xgradle.processors.DefaultPluginPomProcessor;
import org.altlinux.xgradle.redactors.DefaultParentRemover;
import org.altlinux.xgradle.registrars.XmvnBomCompatRegistrar;
import org.altlinux.xgradle.registrars.XmvnCompatRegistrar;
import org.altlinux.xgradle.services.PomProcessingService;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;

/**
 * Google Guice dependency injection module for XGradle tool.
 * Configures bindings for all interfaces and their implementations.
 *
 * @author Ivan Khanas
 */
public class XGradleToolModule extends AbstractModule {
    private final ToolConfig toolConfig;

    /**
     * Constructs a new XGradleToolModule with the specified configuration.
     *
     * @param toolConfig configuration for the tool
     */
    public XGradleToolModule(ToolConfig toolConfig) {
        this.toolConfig = toolConfig;
    }

    /**
     * Configures the dependency injection bindings.
     * Binds interfaces to their implementations with appropriate naming.
     */
    @Override
    protected void configure() {
        bind(ArtifactCollector.class).to(DefaultArtifactCollector.class);
        bind(PomCollector.class).to(DefaultPomCollector.class);

        bind(ArtifactContainer.class).to(DefaultArtifactContainer.class);
        bind(PomContainer.class).to(DefaultPomContainer.class);

        bind(new TypeLiteral<PomParser<HashMap<String, Path>>>() {})
                .annotatedWith(Names.named("Library"))
                .to(ConcurrentLibraryPomParser.class);

        bind(new TypeLiteral<PomParser<Set<Path>>>() {})
                .annotatedWith(Names.named("Bom"))
                .to(ConcurrentBomParser.class);

        bind(new TypeLiteral<PomParser<HashMap<String, Path>>>() {})
                .annotatedWith(Names.named("gradlePlugins"))
                .to(DefaultPluginPomParser.class);

        bind(new TypeLiteral<PomParser<HashMap<String, Path>>>() {})
                .annotatedWith(Names.named("Javadoc"))
                .to(ConcurrentJavadocParser.class);

        bind(new TypeLiteral<PomProcessor<HashMap<String, Path>>>() {})
                .annotatedWith(Names.named("Library"))
                .to(DefaultLibraryPomProcessor.class);

        bind(new TypeLiteral<PomProcessor<Set<Path>>>() {})
                .annotatedWith(Names.named("Bom"))
                .to(DefaultBomProcessor.class);

        bind(new TypeLiteral<PomProcessor<HashMap<String, Path>>>() {})
                .annotatedWith(Names.named("gradlePlugins"))
                .to(DefaultPluginPomProcessor.class);

        bind(new TypeLiteral<PomProcessor<HashMap<String, Path>>>() {})
                .annotatedWith(Names.named("Javadoc"))
                .to(DefaultJavadocProcessor.class);

        bind(CommandLineParser.class).to(DefaultCommandLineParser.class);
        bind(CommandExecutor.class).to(DefaultCommandExecutor.class);

        bind(Registrar.class).annotatedWith(Names.named("Library")).to(XmvnCompatRegistrar.class);
        bind(Registrar.class).annotatedWith(Names.named("Bom")).to(XmvnBomCompatRegistrar.class);

        bind(XmvnCompatController.class).annotatedWith(Names.named("Library")).to(DefaultXmvnCompatController.class);
        bind(XmvnCompatController.class).annotatedWith(Names.named("Bom")).to(DefaultBomXmvnCompatController.class);
        bind(XmvnCompatController.class).annotatedWith(Names.named("Javadoc")).to(DefaultJavadocXmvnCompatController.class);

        bind(ParentRedactor.class).annotatedWith(Names.named("Remove")).to(DefaultParentRemover.class);

        bind(ArtifactsInstaller.class).to(DefaultPluginArtifactsInstaller.class);
        bind(ArtifactsInstallationController.class).to(DefaultPluginsInstallationController.class);

        bind(JavadocInstaller.class).to(DefaultJavadocInstaller.class);
        bind(XmvnCompatController.class).annotatedWith(Names.named("Javadoc")).to(DefaultJavadocXmvnCompatController.class);

        bind(PomService.class).to(PomProcessingService.class);

        bind(ArtifactCache.class).to(DefaultArtifactCache.class);
        bind(ToolConfig.class).toInstance(toolConfig);
    }
}