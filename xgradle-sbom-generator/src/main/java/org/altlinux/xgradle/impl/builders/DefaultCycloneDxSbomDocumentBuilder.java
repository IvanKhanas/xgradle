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
package org.altlinux.xgradle.impl.builders;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.impl.enums.SbomComponentKind;
import org.altlinux.xgradle.impl.enums.SbomFormat;
import org.altlinux.xgradle.impl.models.SbomComponent;
import org.altlinux.xgradle.impl.validation.SbomValidationUtils;
import org.altlinux.xgradle.interfaces.builders.SbomDocumentBuilder;

import java.time.Instant;
import java.util.List;

/**
 * Builds CycloneDX report JSON document from normalized SBOM components.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultCycloneDxSbomDocumentBuilder implements SbomDocumentBuilder {

    @Inject
    DefaultCycloneDxSbomDocumentBuilder() {
    }

    @Override
    public SbomFormat format() {
        return SbomFormat.CYCLONEDX;
    }

    @Override
    public JsonObject build(
            String projectName,
            String projectVersion,
            List<SbomComponent> components
    ) {
        String now = Instant.now().toString();

        JsonObject document = new JsonObject();
        document.addProperty("bomFormat", "CycloneDX");
        document.addProperty("specVersion", "1.5");
        document.addProperty("version", 1);

        JsonObject metadata = new JsonObject();
        metadata.addProperty("timestamp", now);

        JsonArray tools = new JsonArray();
        JsonObject tool = new JsonObject();
        tool.addProperty("vendor", "BaseALT");
        tool.addProperty("name", "xgradle-sbom-generator");
        tool.addProperty("version", projectVersion);
        tools.add(tool);
        metadata.add("tools", tools);

        JsonObject rootComponent = new JsonObject();
        rootComponent.addProperty("type", "application");
        rootComponent.addProperty("name", projectName);
        rootComponent.addProperty("version", projectVersion);
        metadata.add("component", rootComponent);

        document.add("metadata", metadata);

        JsonArray componentsArray = new JsonArray();
        components.stream().forEach(component -> {
            JsonObject componentObject = new JsonObject();
            componentObject.addProperty("type", toCycloneDxType(component));

            if (component.getGroupId() != null) {
                componentObject.addProperty("group", component.getGroupId());
            }

            componentObject.addProperty(
                    "name",
                    component.getArtifactId() != null ? component.getArtifactId() : component.displayName()
            );

            if (component.getVersion() != null) {
                componentObject.addProperty("version", component.getVersion());
            }

            if (component.getGroupId() != null
                    && component.getArtifactId() != null
                    && component.getVersion() != null) {
                componentObject.addProperty(
                        "purl",
                        "pkg:maven/" + component.getGroupId()
                                + "/"
                                + component.getArtifactId()
                                + "@"
                                + component.getVersion()
                );
            }

            addCycloneDxExternalReferences(componentObject, component);
            addCycloneDxLicenses(componentObject, component);
            addCycloneDxProperties(componentObject, component);
            componentsArray.add(componentObject);
        });

        document.add("components", componentsArray);
        return document;
    }

    private void addCycloneDxExternalReferences(
            JsonObject componentObject,
            SbomComponent component
    ) {
        JsonArray references = new JsonArray();
        addCycloneDxExternalReference(references, "website", component.getProjectUrl());
        addCycloneDxExternalReference(references, "vcs", component.getScmUrl());

        if (references.size() > 0) {
            componentObject.add("externalReferences", references);
        }
    }

    private void addCycloneDxExternalReference(
            JsonArray references,
            String type,
            String url
    ) {
        String normalizedUrl = SbomValidationUtils.normalizeNullable(url);
        if (normalizedUrl == null) {
            return;
        }

        JsonObject reference = new JsonObject();
        reference.addProperty("type", type);
        reference.addProperty("url", normalizedUrl);
        references.add(reference);
    }

    private void addCycloneDxLicenses(
            JsonObject componentObject,
            SbomComponent component
    ) {
        if (component.getLicenses() == null || component.getLicenses().isEmpty()) {
            return;
        }

        JsonArray licenses = new JsonArray();
        component.getLicenses().stream()
                .filter(license -> license != null)
                .forEach(license -> {
                    JsonObject inner = new JsonObject();
                    String name = SbomValidationUtils.normalizeNullable(license.getName());
                    String url = SbomValidationUtils.normalizeNullable(license.getUrl());
                    if (name != null) {
                        inner.addProperty("name", name);
                    }
                    if (url != null) {
                        inner.addProperty("url", url);
                    }
                    if (inner.size() == 0) {
                        return;
                    }

                    JsonObject wrapper = new JsonObject();
                    wrapper.add("license", inner);
                    licenses.add(wrapper);
                });

        if (licenses.size() > 0) {
            componentObject.add("licenses", licenses);
        }
    }

    private void addCycloneDxProperties(
            JsonObject componentObject,
            SbomComponent component
    ) {
        if (component.getComponentKind() != SbomComponentKind.GRADLE_PLUGIN) {
            return;
        }

        JsonArray properties = new JsonArray();
        JsonObject property = new JsonObject();
        property.addProperty("name", "xgradle:component-kind");
        property.addProperty("value", "gradle-plugin");
        properties.add(property);
        componentObject.add("properties", properties);
    }

    private String toCycloneDxType(SbomComponent component) {
        SbomComponentKind componentKind = component.getComponentKind();
        if (componentKind == SbomComponentKind.FILE) {
            return "file";
        }
        if (componentKind == SbomComponentKind.GRADLE_PLUGIN) {
            return "framework";
        }
        return "library";
    }
}
