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

package org.altlinux.xgradle.interfaces.collectors;

import java.nio.file.Path;
import java.util.List;

/**
 * Collector for POM files in a repository directory.
 * <p>
 * Responsible for scanning the filesystem and returning
 * a list of all POM files under the given root directory.
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface PomFilesCollector extends Collector<Path, List<Path>> {

    /**
     * Collects POM files under the given root directory.
     *
     * @param rootDirectory root directory to scan
     * @return list of discovered POM files
     */
    @Override
    List<Path> collect(Path rootDirectory);
}
