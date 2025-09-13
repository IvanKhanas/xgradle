package org.altlinux.xgradle.di;

import com.google.inject.AbstractModule;

import org.altlinux.xgradle.api.cli.CommandCreator;
import org.altlinux.xgradle.api.cli.CommandExecutor;
import org.altlinux.xgradle.api.cli.CommandLineParser;
import org.altlinux.xgradle.api.parsers.PomParser;
import org.altlinux.xgradle.api.processors.PomProcessor;
import org.altlinux.xgradle.api.collectors.ArtifactCollector;
import org.altlinux.xgradle.api.collectors.PomCollector;
import org.altlinux.xgradle.api.containers.ArtifactContainer;
import org.altlinux.xgradle.api.containers.PomContainer;
import org.altlinux.xgradle.cli.DefaultCommandCreator;
import org.altlinux.xgradle.cli.DefaultCommandExecutor;
import org.altlinux.xgradle.cli.DefaultCommandLineParser;
import org.altlinux.xgradle.collectors.DefaultArtifactCollector;
import org.altlinux.xgradle.collectors.DefaultPomCollector;
import org.altlinux.xgradle.containers.DefaultArtifactContainer;
import org.altlinux.xgradle.containers.DefaultPomContainer;
import org.altlinux.xgradle.parsers.ConcurrentPomParser;
import org.altlinux.xgradle.processors.DefaultPomProcessor;

public class XGradleToolModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ArtifactCollector.class).to(DefaultArtifactCollector.class);
        bind(PomCollector.class).to(DefaultPomCollector.class);

        bind(ArtifactContainer.class).to(DefaultArtifactContainer.class);
        bind(PomContainer.class).to(DefaultPomContainer.class);

        bind(PomParser.class).to(ConcurrentPomParser.class);
        bind(PomProcessor.class).to(DefaultPomProcessor.class);

        bind(CommandLineParser.class).to(DefaultCommandLineParser.class);
        bind(CommandCreator.class).to(DefaultCommandCreator.class);
        bind(CommandExecutor.class).to(DefaultCommandExecutor.class);
    }
}
