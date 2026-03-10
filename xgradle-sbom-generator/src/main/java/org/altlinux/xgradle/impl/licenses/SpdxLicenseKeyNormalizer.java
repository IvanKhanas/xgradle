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

import org.altlinux.xgradle.impl.validation.SbomValidationUtils;

import java.util.Set;

/**
 * Shared key normalization helpers for SPDX license lookup.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class SpdxLicenseKeyNormalizer {

    private static final Set<String> NAME_STOP_WORDS = Set.of(
            "the",
            "license",
            "licence",
            "software",
            "version",
            "v"
    );

    @Inject
    SpdxLicenseKeyNormalizer() {
    }

    String lookup(String value) {
        String normalized = SbomValidationUtils.normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        return normalized.toLowerCase(SbomValidationUtils.ROOT_LOCALE);
    }

    String nameKey(String value) {
        String normalized = lookup(value);
        if (normalized == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder(normalized.length());
        boolean previousWasSeparator = false;
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (isAsciiLetterOrDigit(current)) {
                builder.append(current);
                previousWasSeparator = false;
                continue;
            }

            if (!previousWasSeparator) {
                builder.append(' ');
                previousWasSeparator = true;
            }
        }

        return trimToNull(builder.toString());
    }

    String reducedNameKey(String value) {
        String normalized = nameKey(value);
        if (normalized == null) {
            return null;
        }

        StringBuilder reduced = new StringBuilder(normalized.length());
        for (String token : normalized.split(" ")) {
            if (token.isBlank() || NAME_STOP_WORDS.contains(token)) {
                continue;
            }
            if (reduced.length() > 0) {
                reduced.append(' ');
            }
            reduced.append(token);
        }

        return trimToNull(reduced.toString());
    }

    String urlKey(String value) {
        String normalized = lookup(value);
        if (normalized == null || normalized.contains("${")) {
            return null;
        }

        String normalizedUrl = stripSuffix(normalized, '#');
        normalizedUrl = stripSuffix(normalizedUrl, '?');
        normalizedUrl = dropPrefix(normalizedUrl, "git+");
        normalizedUrl = dropPrefix(normalizedUrl, "https://");
        normalizedUrl = dropPrefix(normalizedUrl, "http://");
        normalizedUrl = dropPrefix(normalizedUrl, "www.");

        while (normalizedUrl.endsWith("/")) {
            normalizedUrl = normalizedUrl.substring(0, normalizedUrl.length() - 1);
        }

        return trimToNull(normalizedUrl);
    }

    private boolean isAsciiLetterOrDigit(char value) {
        return (value >= 'a' && value <= 'z') || (value >= '0' && value <= '9');
    }

    private String dropPrefix(
            String value,
            String prefix
    ) {
        if (value.startsWith(prefix)) {
            return value.substring(prefix.length());
        }
        return value;
    }

    private String stripSuffix(
            String value,
            char separator
    ) {
        int index = value.indexOf(separator);
        if (index >= 0) {
            return value.substring(0, index);
        }
        return value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        StringBuilder collapsed = new StringBuilder(trimmed.length());
        boolean previousWasSpace = false;
        for (int index = 0; index < trimmed.length(); index++) {
            char current = trimmed.charAt(index);
            if (Character.isWhitespace(current)) {
                if (!previousWasSpace) {
                    collapsed.append(' ');
                    previousWasSpace = true;
                }
                continue;
            }
            collapsed.append(current);
            previousWasSpace = false;
        }
        return collapsed.toString();
    }
}
