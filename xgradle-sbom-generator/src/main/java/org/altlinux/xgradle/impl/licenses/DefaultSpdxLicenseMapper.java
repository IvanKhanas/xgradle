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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.impl.models.SbomLicense;
import org.altlinux.xgradle.interfaces.licenses.SpdxLicenseMapper;

import java.util.Optional;

/**
 * Default SPDX mapper backed by bundled SPDX license catalog.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultSpdxLicenseMapper implements SpdxLicenseMapper {

    private final SpdxLicenseCatalog catalog;
    private final SpdxLicenseKeyNormalizer keyNormalizer;

    @Inject
    DefaultSpdxLicenseMapper(
            SpdxLicenseCatalogLoader catalogLoader,
            SpdxLicenseKeyNormalizer keyNormalizer
    ) {
        this.catalog = catalogLoader.getCatalog();
        this.keyNormalizer = keyNormalizer;
    }

    @Override
    public Optional<String> resolve(SbomLicense license) {
        if (license == null) {
            return Optional.empty();
        }

        String mappedFromName = resolveByName(license.getName());
        if (mappedFromName != null) {
            return Optional.of(mappedFromName);
        }

        String mappedFromUrl = resolveByUrl(license.getUrl());
        if (mappedFromUrl != null) {
            return Optional.of(mappedFromUrl);
        }

        return Optional.empty();
    }

    private String resolveByName(String name) {
        String lookupKey = keyNormalizer.lookup(name);
        if (lookupKey == null) {
            return null;
        }

        String byIdentifier = catalog.byIdentifier(lookupKey);
        if (byIdentifier != null) {
            return byIdentifier;
        }

        String byName = catalog.byName(keyNormalizer.nameKey(name));
        if (byName != null) {
            return byName;
        }

        return catalog.byReducedName(keyNormalizer.reducedNameKey(name));
    }

    private String resolveByUrl(String url) {
        return catalog.byUrl(keyNormalizer.urlKey(url));
    }
}
