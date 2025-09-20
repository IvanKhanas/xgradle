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
package org.altlinux.xgradle;

import com.beust.jcommander.JCommander;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.altlinux.xgradle.api.controllers.XmvnCompatController;
import org.altlinux.xgradle.cli.CliArgumentsContainer;
import org.altlinux.xgradle.cli.CustomXgradleFormatter;
import org.altlinux.xgradle.controllers.DefaultPluginsInstallationController;
import org.altlinux.xgradle.controllers.DefaultXmvnCompatController;
import org.altlinux.xgradle.di.XGradleToolModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger("XgradleLogger");

    public static void main(String[] args) {
        try {
            CliArgumentsContainer arguments = new CliArgumentsContainer();
            JCommander jCommander = JCommander.newBuilder()
                    .addObject(arguments)
                    .programName("xgradle-tool")
                    .build();

            jCommander.setUsageFormatter(new CustomXgradleFormatter(jCommander));


            try {
                jCommander.parse(args);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            Injector injector = Guice.createInjector(new XGradleToolModule());
            XmvnCompatController xmvnController = injector.getInstance(DefaultXmvnCompatController.class);
            DefaultPluginsInstallationController pluginsController = injector.getInstance(DefaultPluginsInstallationController.class);

            xmvnController.configure().configureXmvnCompatFunctions(jCommander, args, arguments, logger);
            pluginsController.configure().configurePluginArtifactsInstallation(jCommander, args, arguments, logger);
        } catch (Exception e) {
            logger.error("Fatal error occurred", e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}