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
package org.altlinux.xgradle.impl.model;

import org.altlinux.xgradle.impl.enums.MavenPackaging;
import org.apache.maven.model.Model;

import java.nio.file.Path;

/**
 * Creates MavenCoordinate instances from effective Maven models.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public final class PomCoordinateFactory {

    public MavenCoordinate create(Model effectiveModel, Path pomPath) {
        if (effectiveModel == null) {
            return null;
        }

        MavenCoordinateBuilder builder = MavenCoordinate.builder()
                .pomPath(pomPath)
                .artifactId(effectiveModel.getArtifactId())
                .groupId(effectiveModel.getGroupId())
                .version(effectiveModel.getVersion())
                .packaging(
                        isEmpty(effectiveModel.getPackaging())
                                ? MavenPackaging.JAR.getPackaging()
                                : effectiveModel.getPackaging()
                );

        if (isEmpty(effectiveModel.getGroupId())
                && effectiveModel.getParent() != null) {
            builder.groupId(
                    effectiveModel.getParent().getGroupId()
            );
        }

        if (isEmpty(effectiveModel.getVersion())
                && effectiveModel.getParent() != null) {
            builder.version(
                    effectiveModel.getParent().getVersion()
            );
        }

        return builder.build();
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
