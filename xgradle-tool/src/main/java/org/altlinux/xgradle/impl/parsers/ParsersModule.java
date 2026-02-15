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
package org.altlinux.xgradle.impl.parsers;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

import org.altlinux.xgradle.interfaces.parsers.PomParser;

import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Bom;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.GradlePlugin;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Javadoc;
import org.altlinux.xgradle.impl.bindingannotations.processingtypes.Library;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;
/**
 * Guice module for Parsers bindings.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

public final class ParsersModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(new TypeLiteral<PomParser<HashMap<String, Path>>>() {})
                .annotatedWith(Library.class)
                .to(ConcurrentLibraryPomParser.class);

        bind(new TypeLiteral<PomParser<Set<Path>>>() {})
                .annotatedWith(Bom.class)
                .to(ConcurrentBomParser.class);

        bind(new TypeLiteral<PomParser<HashMap<String, Path>>>() {})
                .annotatedWith(GradlePlugin.class)
                .to(DefaultPluginPomParser.class);

        bind(new TypeLiteral<PomParser<HashMap<String, Path>>>() {})
                .annotatedWith(Javadoc.class)
                .to(ConcurrentJavadocParser.class);
    }
}
