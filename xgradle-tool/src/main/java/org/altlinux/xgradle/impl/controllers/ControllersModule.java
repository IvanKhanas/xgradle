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
package org.altlinux.xgradle.impl.controllers;

import com.google.inject.AbstractModule;

import org.altlinux.xgradle.interfaces.controllers.ArtifactsInstallationController;
import org.altlinux.xgradle.interfaces.controllers.PomRedactionController;
import org.altlinux.xgradle.interfaces.controllers.XmvnCompatController;

import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Bom;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Javadoc;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Library;
/**
 * Guice module for Controllers bindings.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

public final class ControllersModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(XmvnCompatController.class)
                .annotatedWith(Library.class)
                .to(DefaultXmvnCompatController.class);

        bind(XmvnCompatController.class)
                .annotatedWith(Bom.class)
                .to(DefaultBomXmvnCompatController.class);

        bind(XmvnCompatController.class)
                .annotatedWith(Javadoc.class)
                .to(DefaultJavadocXmvnCompatController.class);

        bind(ArtifactsInstallationController.class)
                .to(DefaultPluginsInstallationController.class);

        bind(PomRedactionController.class).to(DefaultPomRedactionController.class);
    }
}
