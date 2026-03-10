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
package org.altlinux.xgradle.impl.preprocessors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.impl.models.SbomComponent;
import org.altlinux.xgradle.interfaces.preprocessors.SbomComponentPreprocessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Sorts and normalizes SBOM component collection before report generation.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultSbomComponentPreprocessor implements SbomComponentPreprocessor {

    @Inject
    DefaultSbomComponentPreprocessor() {
    }

    @Override
    public List<SbomComponent> preprocess(Collection<SbomComponent> components) {
        List<SbomComponent> orderedComponents = new ArrayList<>();
        if (components != null) {
            orderedComponents.addAll(components);
        }

        orderedComponents.sort(Comparator
                .comparing((SbomComponent component) -> nullToEmpty(component.getGroupId()))
                .thenComparing(component -> nullToEmpty(component.getArtifactId()))
                .thenComparing(component -> nullToEmpty(component.getVersion()))
                .thenComparing(component -> nullToEmpty(component.getFileName()))
        );

        return orderedComponents;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
