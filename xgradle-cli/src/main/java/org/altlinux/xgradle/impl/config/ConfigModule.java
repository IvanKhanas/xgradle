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
package org.altlinux.xgradle.impl.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.altlinux.xgradle.impl.cli.CliArgumentsContainer;
/**
 * Guice module for Config bindings.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

public final class ConfigModule extends AbstractModule {

    @Provides
    @Singleton
    ToolConfig toolConfig(CliArgumentsContainer args) {
        return new ToolConfig(args);
    }
}
