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

import com.google.inject.name.Names;
import org.altlinux.xgradle.api.cli.CommandCreator;
import org.altlinux.xgradle.api.cli.CommandExecutor;
import org.altlinux.xgradle.api.cli.CommandLineParser;
import org.altlinux.xgradle.api.controllers.ArtifactsInstallationController;
import org.altlinux.xgradle.api.controllers.XmvnCompatController;
import org.altlinux.xgradle.api.install.ArtifactsInstaller;
import org.altlinux.xgradle.api.parsers.PomParser;
import org.altlinux.xgradle.api.processors.PomProcessor;
import org.altlinux.xgradle.api.collectors.ArtifactCollector;
import org.altlinux.xgradle.api.collectors.PomCollector;
import org.altlinux.xgradle.api.containers.ArtifactContainer;
import org.altlinux.xgradle.api.containers.PomContainer;
import org.altlinux.xgradle.api.registrars.Registrar;
import org.altlinux.xgradle.cli.DefaultCommandCreator;
import org.altlinux.xgradle.cli.DefaultCommandExecutor;
import org.altlinux.xgradle.cli.DefaultCommandLineParser;
import org.altlinux.xgradle.collectors.DefaultArtifactCollector;
import org.altlinux.xgradle.collectors.DefaultPomCollector;
import org.altlinux.xgradle.containers.DefaultArtifactContainer;
import org.altlinux.xgradle.containers.DefaultPomContainer;
import org.altlinux.xgradle.controllers.DefaultPluginsInstallationController;
import org.altlinux.xgradle.controllers.DefaultXmvnCompatController;
import org.altlinux.xgradle.installers.DefaultPluginArtifactsInstaller;
import org.altlinux.xgradle.parsers.ConcurrentLibraryPomParser;
import org.altlinux.xgradle.parsers.PluginPomsParser;
import org.altlinux.xgradle.processors.DefaultPomProcessor;
import org.altlinux.xgradle.processors.PluginPomsProcessor;
import org.altlinux.xgradle.registrars.XmvnCompatRegistrar;

public class XGradleToolModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ArtifactCollector.class).to(DefaultArtifactCollector.class);
        bind(PomCollector.class).to(DefaultPomCollector.class);

        bind(ArtifactContainer.class).to(DefaultArtifactContainer.class);
        bind(PomContainer.class).to(DefaultPomContainer.class);

        bind(PomParser.class).annotatedWith(Names.named("Library")).to(ConcurrentLibraryPomParser.class);
        bind(PomParser.class).annotatedWith(Names.named("gradlePlugins")).to(PluginPomsParser.class);

        bind(PomProcessor.class).annotatedWith(Names.named("Library")).to(DefaultPomProcessor.class);
        bind(PomProcessor.class).annotatedWith(Names.named("gradlePlugins")).to(PluginPomsProcessor.class);

        bind(CommandLineParser.class).to(DefaultCommandLineParser.class);
        bind(CommandCreator.class).to(DefaultCommandCreator.class);
        bind(CommandExecutor.class).to(DefaultCommandExecutor.class);

        bind(Registrar.class).to(XmvnCompatRegistrar.class);
        bind(XmvnCompatController.class).to(DefaultXmvnCompatController.class);

        bind(ArtifactsInstaller.class).to(DefaultPluginArtifactsInstaller.class);
        bind(ArtifactsInstallationController.class).to(DefaultPluginsInstallationController.class);
    }
}
