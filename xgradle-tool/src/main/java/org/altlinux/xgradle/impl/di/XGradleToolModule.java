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
package org.altlinux.xgradle.impl.di;

import com.google.inject.AbstractModule;


import org.altlinux.xgradle.impl.application.ApplicationModule;
import org.altlinux.xgradle.impl.caches.CachesModule;
import org.altlinux.xgradle.impl.cli.CliModule;
import org.altlinux.xgradle.impl.collectors.CollectorsModule;
import org.altlinux.xgradle.impl.config.ConfigModule;

import org.altlinux.xgradle.impl.containers.ContainersModule;

import org.altlinux.xgradle.impl.controllers.*;

import org.altlinux.xgradle.impl.installers.InstallersModule;
import org.altlinux.xgradle.impl.model.ModelModule;
import org.altlinux.xgradle.impl.parsers.*;
import org.altlinux.xgradle.impl.processors.*;
import org.altlinux.xgradle.impl.redactors.RedactorsModule;
import org.altlinux.xgradle.impl.registrars.RegistrarsModule;

import org.altlinux.xgradle.impl.services.ServicesModule;

/**
 * Google Guice dependency injection module for XGradle tool.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public final class XGradleToolModule extends AbstractModule {

    @Override
    protected void configure() {

        install(new ModelModule());

        install(new CollectorsModule());

        install(new ContainersModule());

        install(new ParsersModule());

        install(new ProcessorsModule());

        install(new CliModule());

        install(new RegistrarsModule());

        install(new ControllersModule());

        install(new RedactorsModule());

        install(new InstallersModule());

        install(new ServicesModule());

        install(new CachesModule());

        install(new ConfigModule());

        install(new ApplicationModule());
    }
}
