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
package org.altlinux.xgradle.impl.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.internal.DefaultConsole;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.cli.CommandExecutor;
import org.altlinux.xgradle.interfaces.cli.CommandLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Guice module for CLI bindings.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

public final class CliModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(CommandExecutor.class).to(DefaultCommandExecutor.class);
        bind(CommandLineParser.class).to(DefaultCommandLineParser.class);
    }

    @Provides
    @Singleton
    Logger logger() {
        return LoggerFactory.getLogger("XGradleLogger");
    }


    @Provides
    @Singleton
    JCommander jCommander(CliArgumentsContainer args) {
        JCommander jc = JCommander.newBuilder()
                .addObject(args)
                .programName("xgradle-tool")
                .console(new DefaultConsole())
                .build();

        jc.setUsageFormatter(new CustomXgradleFormatter(jc));
        return jc;
    }
}
