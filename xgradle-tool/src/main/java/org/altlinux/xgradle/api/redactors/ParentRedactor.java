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
package org.altlinux.xgradle.api.redactors;

import java.nio.file.Path;

/**
 * Interface for POM parent block redaction operations.
 * Defines contract for removing parent blocks from POM files.
 *
 * @author Ivan Khanas
 */
public interface ParentRedactor{

    /**
     * Removes the parent block from the specified POM file.
     *
     * @param pomPath path to the POM file to modify
     */
    void removeParent(Path pomPath);
}
