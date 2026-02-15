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
package org.altlinux.xgradle.impl.installers;

import com.google.inject.AbstractModule;
import org.altlinux.xgradle.interfaces.installers.ArtifactsInstaller;
import org.altlinux.xgradle.interfaces.installers.JavadocInstaller;
/**
 * Guice module for Installers bindings.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

public final class InstallersModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ArtifactsInstaller.class).to(DefaultPluginArtifactsInstaller.class);
        bind(JavadocInstaller.class).to(DefaultJavadocInstaller.class);
    }
}
