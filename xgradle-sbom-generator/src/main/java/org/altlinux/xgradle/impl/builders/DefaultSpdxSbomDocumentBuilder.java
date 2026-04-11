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

import org.altlinux.xgradle.impl.enums.SbomFormat;
import org.altlinux.xgradle.impl.models.SbomComponent;
import org.altlinux.xgradle.impl.models.SbomLicense;
import org.altlinux.xgradle.impl.validation.SbomValidationUtils;
import org.altlinux.xgradle.interfaces.builders.SbomDocumentBuilder;
import org.altlinux.xgradle.interfaces.licenses.SpdxLicenseMapper;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Builds SPDX report JSON document from normalized SBOM components.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultSpdxSbomDocumentBuilder implements SbomDocumentBuilder {

    private final SpdxLicenseMapper spdxLicenseMapper;

    @Inject
    DefaultSpdxSbomDocumentBuilder(SpdxLicenseMapper spdxLicenseMapper) {
        this.spdxLicenseMapper = spdxLicenseMapper;
    }

    @Override
    public SbomFormat format() {
        return SbomFormat.SPDX;
    }

    @Override
    public JsonObject build(
            String projectName,
            String projectVersion,
            List<SbomComponent> components
    ) {
        String now = Instant.now().toString();

        JsonObject document = new JsonObject();
        document.addProperty("spdxVersion", "SPDX-2.3");
        document.addProperty("dataLicense", "CC0-1.0");
        document.addProperty("SPDXID", "SPDXRef-DOCUMENT");
        document.addProperty("name", projectName + "-sbom");
        document.addProperty("documentNamespace", "https://altlinux.org/xgradle/sbom/" + projectName + "/" + now);

        JsonObject creationInfo = new JsonObject();
        creationInfo.addProperty("created", now);
        JsonArray creators = new JsonArray();
        creators.add("Tool: xgradle-sbom-generator " + projectVersion);
        creationInfo.add("creators", creators);
        document.add("creationInfo", creationInfo);

        JsonArray packages = new JsonArray();
        IntStream.range(0, components.size()).forEach(index -> {
            SbomComponent component = components.get(index);
            JsonObject packageObject = new JsonObject();
            packageObject.addProperty("name", component.displayName());
            packageObject.addProperty("SPDXID", "SPDXRef-Package-" + (index + 1));
            packageObject.addProperty(
                    "versionInfo",
                    component.getVersion() != null ? component.getVersion() : "NOASSERTION"
            );
            packageObject.addProperty("licenseDeclared", toSpdxLicenseExpression(component.getLicenses()));
            packageObject.addProperty(
                    "homepage",
                    firstNonBlank(component.getProjectUrl(), component.getScmUrl(), "NOASSERTION")
            );
            packageObject.addProperty("filesAnalyzed", false);
            packages.add(packageObject);
        });

        document.add("packages", packages);
        return document;
    }

    private String toSpdxLicenseExpression(List<SbomLicense> licenses) {
        if (licenses == null || licenses.isEmpty()) {
            return "NOASSERTION";
        }

        Set<String> identifiers = new LinkedHashSet<>();
        licenses.stream()
                .flatMap(license -> spdxLicenseMapper.resolve(license).stream())
                .forEach(identifiers::add);

        if (identifiers.isEmpty()) {
            return "NOASSERTION";
        }
        return String.join(" OR ", identifiers);
    }

    private String firstNonBlank(
            String first,
            String second,
            String fallback
    ) {
        String firstNormalized = SbomValidationUtils.normalizeNullable(first);
        if (firstNormalized != null) {
            return firstNormalized;
        }

        String secondNormalized = SbomValidationUtils.normalizeNullable(second);
        if (secondNormalized != null) {
            return secondNormalized;
        }

        return fallback;
    }
}
