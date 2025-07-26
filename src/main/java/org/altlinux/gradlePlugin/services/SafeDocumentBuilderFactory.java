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
package org.altlinux.gradlePlugin.services;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Factory class for creating secure XML DocumentBuilder instances
 * with protection against XXE (XML External Entity) attacks.
 *
 * @author Ivan Khanas
 */
public class SafeDocumentBuilderFactory {

    /**
     * Creates and configures a secure DocumentBuilder instance with
     * XXE protection features enabled.
     *
     * <p>The builder is configured with the following security features:
     * <ul>
     *   <li>Disables DOCTYPE declarations</li>
     *   <li>Disables external general entities</li>
     *   <li>Disables external parameter entities</li>
     *   <li>Disables external DTD loading</li>
     *   <li>Disables XInclude processing</li>
     *   <li>Disables entity reference expansion</li>
     * </ul>
     *
     * @return A securely configured DocumentBuilder instance
     *
     * @throws ParserConfigurationException If the requested features are not
     *         supported by the XML parser implementation
     * @throws IllegalStateException If the XXE protection configuration fails
     *         due to underlying parser limitations
     */
    public static DocumentBuilder createBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("XXE protection configuration failed", e);
        }
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder();
    }
}