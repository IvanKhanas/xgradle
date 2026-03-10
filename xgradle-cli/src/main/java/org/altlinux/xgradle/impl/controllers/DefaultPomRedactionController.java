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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.controllers.PomRedactionController;
import org.altlinux.xgradle.impl.config.ToolConfig;
import org.altlinux.xgradle.interfaces.services.PomService;
import org.altlinux.xgradle.impl.cli.CliArgumentsContainer;

import java.nio.file.Path;
import java.util.List;
/**
 * Controller for POM Redaction.
 * Implements {@link PomRedactionController}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

@Singleton
final class DefaultPomRedactionController implements PomRedactionController {

    private final ToolConfig toolConfig;
    private final PomService pomService;
    private final CliArgumentsContainer arguments;

    @Inject
    DefaultPomRedactionController(ToolConfig toolConfig, PomService pomService, CliArgumentsContainer arguments) {
        this.toolConfig = toolConfig;
        this.pomService = pomService;
        this.arguments = arguments;
    }

    @Override
    public void configure() {
        Path path = Path.of(arguments.getSearchingDirectory());
        boolean recursive = toolConfig.isRecursive();

        if (arguments.hasRemoveDependencies()) {
            arguments.getRemoveDependencies().forEach(c -> pomService.removeDependency(path, c, recursive));
            return;
        }

        if (arguments.hasAddDependencies()) {
            arguments.getAddDependencies().forEach(c -> pomService.addDependency(path, c, recursive));
            return;
        }

        if (arguments.hasChangeDependencies()) {
            List<String> pair = arguments.getChangeDependencies();
            pomService.changeDependency(path, pair.get(0), pair.get(1), recursive);
        }
    }
}
