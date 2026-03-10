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

package org.altlinux.xgradle.impl.utils.logging;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
/**
 * Guice module for Logging bindings.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

public final class LoggingModule extends AbstractModule {

    @Provides
    @Singleton
    Logger provideLogger() {
        return Logging.getLogger("XGradleLogger");
    }
}
