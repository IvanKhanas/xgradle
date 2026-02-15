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

package org.altlinux.xgradle.interfaces.managers;

import org.gradle.api.initialization.Settings;

/**
 * Manager for plugin repositories and plugin resolution pipeline.
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface PluginManager extends Manager<Settings>{

    /**
     * Configures plugin repositories and resolution for the given settings.
     *
     * @param settings Gradle settings instance
     */
    @Override
    void configure(Settings settings);
}
