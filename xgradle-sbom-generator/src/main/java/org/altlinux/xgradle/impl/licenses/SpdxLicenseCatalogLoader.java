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
package org.altlinux.xgradle.impl.licenses;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.impl.validation.SbomValidationUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Loads SPDX license catalog from bundled runtime resource.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class SpdxLicenseCatalogLoader {

    private static final String SPDX_RESOURCE = "spdx-licanses.json";

    private final SpdxLicenseKeyNormalizer keyNormalizer;
    private final SpdxLicenseCatalog catalog;

    @Inject
    SpdxLicenseCatalogLoader(SpdxLicenseKeyNormalizer keyNormalizer) {
        this.keyNormalizer = keyNormalizer;
        this.catalog = loadCatalog();
    }

    SpdxLicenseCatalog getCatalog() {
        return catalog;
    }

    private SpdxLicenseCatalog loadCatalog() {
        try (InputStream inputStream = openResource(SPDX_RESOURCE);
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                throw new IllegalStateException("Invalid SPDX resource root: " + SPDX_RESOURCE);
            }

            JsonArray licenses = rootElement.getAsJsonObject().getAsJsonArray("licenses");
            if (licenses == null) {
                throw new IllegalStateException("SPDX resource has no 'licenses' array: " + SPDX_RESOURCE);
            }

            CatalogBuilder builder = new CatalogBuilder();
            licenses.asList().stream()
                    .filter(JsonElement::isJsonObject)
                    .forEach(entry -> indexLicenseEntry(entry.getAsJsonObject(), builder));

            return builder.build();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load SPDX resource: " + SPDX_RESOURCE, e);
        }
    }

    private InputStream openResource(String resourceName) {
        InputStream inputStream = SpdxLicenseCatalogLoader.class.getClassLoader().getResourceAsStream(resourceName);
        if (inputStream == null) {
            throw new IllegalStateException("Missing SPDX resource: " + resourceName);
        }
        return inputStream;
    }

    private void indexLicenseEntry(
            JsonObject entry,
            CatalogBuilder builder
    ) {
        String licenseId = normalizedString(entry.get("licenseId"));
        if (licenseId == null) {
            return;
        }

        builder.putIdentifier(keyNormalizer.lookup(licenseId), licenseId);
        builder.putName(keyNormalizer.nameKey(licenseId), licenseId);
        builder.putReducedName(keyNormalizer.reducedNameKey(licenseId), licenseId);

        String licenseName = normalizedString(entry.get("name"));
        builder.putName(keyNormalizer.nameKey(licenseName), licenseId);
        builder.putReducedName(keyNormalizer.reducedNameKey(licenseName), licenseId);

        builder.putUrl(keyNormalizer.urlKey(normalizedString(entry.get("reference"))), licenseId);
        builder.putUrl(keyNormalizer.urlKey(normalizedString(entry.get("detailsUrl"))), licenseId);

        JsonArray seeAlso = entry.getAsJsonArray("seeAlso");
        if (seeAlso == null) {
            return;
        }

        seeAlso.asList().forEach(
                seeAlsoEntry -> builder.putUrl(keyNormalizer.urlKey(normalizedString(seeAlsoEntry)), licenseId)
        );
    }

    private String normalizedString(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return SbomValidationUtils.normalizeNullable(element.getAsString());
    }

    private static final class CatalogBuilder {

        private final Map<String, String> idByIdentifier = new LinkedHashMap<>();
        private final Map<String, String> idByName = new LinkedHashMap<>();
        private final Map<String, String> idByReducedName = new LinkedHashMap<>();
        private final Map<String, String> idByUrl = new LinkedHashMap<>();

        private final Set<String> ambiguousNameKeys = new HashSet<>();
        private final Set<String> ambiguousReducedNameKeys = new HashSet<>();
        private final Set<String> ambiguousUrlKeys = new HashSet<>();

        private void putIdentifier(
                String key,
                String licenseId
        ) {
            if (key == null || licenseId == null) {
                return;
            }
            idByIdentifier.putIfAbsent(key, licenseId);
        }

        private void putName(
                String key,
                String licenseId
        ) {
            putUniqueMapping(key, licenseId, idByName, ambiguousNameKeys);
        }

        private void putReducedName(
                String key,
                String licenseId
        ) {
            putUniqueMapping(key, licenseId, idByReducedName, ambiguousReducedNameKeys);
        }

        private void putUrl(
                String key,
                String licenseId
        ) {
            putUniqueMapping(key, licenseId, idByUrl, ambiguousUrlKeys);
        }

        private void putUniqueMapping(
                String key,
                String licenseId,
                Map<String, String> targetMap,
                Set<String> ambiguousKeys
        ) {
            if (key == null || licenseId == null || ambiguousKeys.contains(key)) {
                return;
            }

            String previous = targetMap.get(key);
            if (previous == null) {
                targetMap.put(key, licenseId);
                return;
            }

            if (previous.equals(licenseId)) {
                return;
            }

            targetMap.remove(key);
            ambiguousKeys.add(key);
        }

        private SpdxLicenseCatalog build() {
            return new SpdxLicenseCatalog(idByIdentifier, idByName, idByReducedName, idByUrl);
        }
    }
}
