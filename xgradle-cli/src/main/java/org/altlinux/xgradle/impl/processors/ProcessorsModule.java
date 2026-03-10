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
package org.altlinux.xgradle.impl.processors;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

import org.altlinux.xgradle.interfaces.processors.PomProcessor;

import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Bom;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.GradlePlugin;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Javadoc;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Library;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;
/**
 * Guice module for Processors bindings.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

public final class ProcessorsModule extends AbstractModule {

    @Override
    protected void configure() {

        bind(new TypeLiteral<PomProcessor<HashMap<String, Path>>>() {})
                .annotatedWith(Library.class)
                .to(DefaultLibraryPomProcessor.class);

        bind(new TypeLiteral<PomProcessor<Set<Path>>>() {})
                .annotatedWith(Bom.class)
                .to(DefaultBomProcessor.class);

        bind(new TypeLiteral<PomProcessor<HashMap<String, Path>>>() {})
                .annotatedWith(GradlePlugin.class)
                .to(DefaultPluginPomProcessor.class);

        bind(new TypeLiteral<PomProcessor<HashMap<String, Path>>>() {})
                .annotatedWith(Javadoc.class)
                .to(DefaultJavadocProcessor.class);
    }
}
