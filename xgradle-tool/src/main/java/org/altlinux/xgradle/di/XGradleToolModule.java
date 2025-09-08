package org.altlinux.xgradle.di;

import com.google.inject.AbstractModule;

import org.altlinux.xgradle.api.processors.PomProcessor;
import org.altlinux.xgradle.api.collectors.ArtifactCollector;
import org.altlinux.xgradle.api.collectors.PomCollector;
import org.altlinux.xgradle.api.containers.ArtifactContainer;
import org.altlinux.xgradle.api.containers.PomContainer;
import org.altlinux.xgradle.api.installers.ArtifactInstaller;
import org.altlinux.xgradle.api.installers.PomInstaller;
import org.altlinux.xgradle.collectors.DefaultArtifactCollector;
import org.altlinux.xgradle.collectors.DefaultPomCollector;
import org.altlinux.xgradle.containers.DefaultArtifactContainer;
import org.altlinux.xgradle.containers.DefaultPomContainer;
import org.altlinux.xgradle.installers.DefaultArtifactInstaller;
import org.altlinux.xgradle.installers.DefaultPomInstaller;
import org.altlinux.xgradle.processors.DefaultPomProcessor;

public class XGradleToolModule extends AbstractModule {

    @Override
    protected void configure() {;
        bind(ArtifactCollector.class).to(DefaultArtifactCollector.class);
        bind(PomCollector.class).to(DefaultPomCollector.class);
        bind(ArtifactContainer.class).to(DefaultArtifactContainer.class);
        bind(PomContainer.class).to(DefaultPomContainer.class);
        bind(ArtifactInstaller.class).to(DefaultArtifactInstaller.class);
        bind(PomInstaller.class).to(DefaultPomInstaller.class);
        bind(PomProcessor.class).to(DefaultPomProcessor.class);
    }
}
