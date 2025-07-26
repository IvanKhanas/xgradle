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

import org.altlinux.gradlePlugin.api.PomParser;
import org.altlinux.gradlePlugin.model.MavenCoordinate;

import org.gradle.api.logging.Logger;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link PomParser} that parses Maven POM files
 * with caching for improved performance.
 *
 * <p>This parser provides:
 * <ul>
 *   <li>Basic POM information parsing</li>
 *   <li>Dependency management section parsing</li>
 *   <li>Dependencies parsing</li>
 *   <li>Properties parsing</li>
 *   <li>Multi-level caching for parsed results</li>
 * </ul>
 *
 * <p>Cache types:
 * <ul>
 *   <li>POM_CACHE: Main POM coordinates</li>
 *   <li>DEP_MGMT_CACHE: Dependency management entries</li>
 *   <li>DEPENDENCIES_CACHE: Direct dependencies</li>
 *   <li>PROPERTIES_CACHE: Properties section</li>
 * </ul>
 *
 * <p>Parsing handles:
 * <ul>
 *   <li>Basic project coordinates</li>
 *   <li>Inheritance from parent POM</li>
 *   <li>Default values for packaging and scope</li>
 *   <li>Namespace-agnostic element lookup</li>
 * </ul>
 *
 * @author Ivan Khanas
 */
public class DefaultPomParser implements PomParser {

    private final Map<String, MavenCoordinate> POM_CACHE = new ConcurrentHashMap<>();
    private final Map<String, ArrayList<MavenCoordinate>> DEP_MGMT_CACHE = new ConcurrentHashMap<>();
    private final Map<String, ArrayList<MavenCoordinate>> DEPENDENCIES_CACHE = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> PROPERTIES_CACHE = new ConcurrentHashMap<>();


    /**
     * Parses the main coordinates from a POM file.
     *
     * <p>Handles:
     * <ul>
     *   <li>Direct child elements (artifactId, groupId, version)</li>
     *   <li>Parent inheritance when groupId/version are missing</li>
     *   <li>Default packaging type (jar)</li>
     *   <li>Validation of required fields</li>
     * </ul>
     *
     * @param pomPath path to the POM file
     * @param logger logger for error reporting
     * @return MavenCoordinate object with parsed data, or null if parsing fails
     */
    @Override
    public MavenCoordinate parsePom(Path pomPath, Logger logger) {
        String cacheKey = pomPath.toString();
        if (POM_CACHE.containsKey(cacheKey)) {
            return POM_CACHE.get(cacheKey);
        }

        try (InputStream is = Files.newInputStream(pomPath)) {
            DocumentBuilder builder = SafeDocumentBuilderFactory.createBuilder();
            Document doc = builder.parse(is);
            Element root = doc.getDocumentElement();

            MavenCoordinate coord = new MavenCoordinate();
            coord.pomPath = pomPath;
            coord.artifactId = getDirectChildText(root, "artifactId");
            coord.version = getDirectChildText(root, "version");
            coord.groupId = getDirectChildText(root, "groupId");
            coord.packaging = getDirectChildText(root, "packaging");
            coord.scope = getDirectChildText(root, "scope");

            if (isEmpty(coord.groupId)) {
                coord.groupId = getNestedText(root, "parent/groupId");
            }
            if (isEmpty(coord.version)) {
                coord.version = getNestedText(root, "parent/version");
            }
            if (isEmpty(coord.packaging)) {
                coord.packaging = "jar";
            }

            if (coord.isValid()) {
                POM_CACHE.put(cacheKey, coord);
                return coord;
            }
        } catch (Exception e) {
            logger.debug("POM parse error: {} - {}", pomPath, e.getMessage());
        }
        return null;
    }


    /**
     * Parses the dependency management section of a POM file.
     *
     * <p>Only processes dependencies declared within:
     * {@code <dependencyManagement><dependencies>}
     *
     * @param pomPath path to the POM file
     * @param logger logger for error reporting
     *
     * @return list of managed dependencies {@code @NotNull}
     */
    @Override
    public ArrayList<MavenCoordinate> parseDependencyManagement(Path pomPath, Logger logger) {
        String cacheKey = pomPath.toString();
        if (DEP_MGMT_CACHE.containsKey(cacheKey)) {
            return DEP_MGMT_CACHE.get(cacheKey);
        }

        ArrayList<MavenCoordinate> dependencies = new ArrayList<>();
        try (InputStream is = Files.newInputStream(pomPath)) {
            DocumentBuilder builder = SafeDocumentBuilderFactory.createBuilder();
            Document doc = builder.parse(is);
            Element root = doc.getDocumentElement();

            Element depMgmt = getDirectChildElement(root, "dependencyManagement");
            if (depMgmt != null) {
                Element depsElement = getDirectChildElement(depMgmt, "dependencies");
                if (depsElement != null) {
                    NodeList deps = depsElement.getElementsByTagNameNS("*", "dependency");
                    for (int i = 0; i < deps.getLength(); i++) {
                        Element dep = (Element) deps.item(i);
                        MavenCoordinate coord = parseDependencyElement(dep);
                        if (coord.isValid()) {
                            dependencies.add(coord);
                        }
                    }
                }
            }

            DEP_MGMT_CACHE.put(cacheKey, dependencies);
            return dependencies;
        } catch (Exception e) {
            return dependencies;
        }
    }


    /**
     * Parses direct dependencies from a POM file.
     *
     * <p>Processes dependencies declared within:
     * {@code <dependencies>}
     *
     * @param pomPath path to the POM file
     * @param logger logger for error reporting
     *
     * @return list of direct dependencies {@code @NotNull}
     */
    @Override
    public ArrayList<MavenCoordinate> parseDependencies(Path pomPath, Logger logger) {
        String cacheKey = pomPath.toString() + "_dependencies";
        if (DEPENDENCIES_CACHE.containsKey(cacheKey)) {
            return DEPENDENCIES_CACHE.get(cacheKey);
        }

        ArrayList<MavenCoordinate> dependencies = new ArrayList<>();
        try (InputStream is = Files.newInputStream(pomPath)) {
            DocumentBuilder builder = SafeDocumentBuilderFactory.createBuilder();
            Document doc = builder.parse(is);
            Element root = doc.getDocumentElement();

            Element depsElement = getDirectChildElement(root, "dependencies");
            if (depsElement == null) return dependencies;

            Map<String, String> properties = parseProperties(pomPath, logger);
            ArrayList<MavenCoordinate> depMgmtList = parseDependencyManagement(pomPath, logger);
            Map<String, String> depMgmtMap = new HashMap<>();
            for (MavenCoordinate coord : depMgmtList) {
                if (coord.groupId != null && coord.artifactId != null && coord.version != null) {
                    depMgmtMap.put(coord.groupId + ":" + coord.artifactId, coord.version);
                }
            }

            NodeList deps = depsElement.getElementsByTagNameNS("*", "dependency");
            for (int i = 0; i < deps.getLength(); i++) {
                Element dep = (Element) deps.item(i);
                MavenCoordinate coord = parseDependencyElement(dep);

                if ((coord.version == null || coord.version.isEmpty()) && coord.groupId != null && coord.artifactId != null) {
                    String key = coord.groupId + ":" + coord.artifactId;
                    coord.version = depMgmtMap.get(key);
                }

                if (coord.version != null && coord.version.contains("${")) {
                    String placeholder = extractPlaceholderName(coord.version);
                    if (properties.containsKey(placeholder)) {
                        coord.version = properties.get(placeholder);
                    }
                }

                if (coord.isValid()) {
                    dependencies.add(coord);
                }
            }

            DEPENDENCIES_CACHE.put(cacheKey, dependencies);
            return dependencies;
        } catch (Exception e) {
            logger.error("Error parsing dependencies from: {}", pomPath, e);
            return dependencies;
        }
    }


    /**
     * Parses properties section of a POM file.
     *
     * <p>Collects all properties declared within:
     * {@code <properties>}
     *
     * @param pomPath path to the POM file
     * @param logger logger for error reporting
     *
     * @return map of property names to values {@code @NotNull}
     */
    @Override
    public Map<String, String> parseProperties(Path pomPath, Logger logger) {
        String cacheKey = pomPath.toString() + "_properties";
        if (PROPERTIES_CACHE.containsKey(cacheKey)) {
            return PROPERTIES_CACHE.get(cacheKey);
        }

        Map<String, String> properties = new HashMap<>();
        try (InputStream is = Files.newInputStream(pomPath)) {
            DocumentBuilder builder = SafeDocumentBuilderFactory.createBuilder();
            Document doc = builder.parse(is);
            Element root = doc.getDocumentElement();

            Element propsElement = getDirectChildElement(root, "properties");
            if (propsElement != null) {
                NodeList children = propsElement.getChildNodes();

                for (int i = 0; i < children.getLength(); i++) {
                    Node node = children.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element prop = (Element) node;
                        String tagName = prop.getLocalName();
                        String value = prop.getTextContent().trim();
                        properties.put(tagName, value);
                    }
                }
            }

            PROPERTIES_CACHE.put(cacheKey, properties);
            return properties;
        } catch (Exception e) {
            return properties;
        }
    }


    /**
     * Parses a single dependency element into MavenCoordinate.
     *
     * <p>Sets default values:
     * <ul>
     *   <li>Scope: "compile" if missing</li>
     *   <li>Packaging: "jar" if missing</li>
     * </ul>
     *
     * @param dep XML element representing a dependency
     *
     * @return MavenCoordinate object with dependency information
     */
    private MavenCoordinate parseDependencyElement(Element dep) {
        MavenCoordinate coord = new MavenCoordinate();
        coord.groupId = getDirectChildText(dep, "groupId");
        coord.artifactId = getDirectChildText(dep, "artifactId");
        coord.version = getDirectChildText(dep, "version");
        coord.scope = getDirectChildText(dep, "scope");
        coord.packaging = getDirectChildText(dep, "type");

        if (coord.scope == null) {
            coord.scope = "compile";
        }
        if (coord.packaging == null) {
            coord.packaging = "jar";
        }
        return coord;
    }


    /**
     * Finds a direct child element by tag name.
     *
     * @param parent parent XML element
     * @param tagName name of child element to find
     *
     * @return child element or null if not found
     */
    private Element getDirectChildElement(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String localName = node.getLocalName();
                if (tagName.equals(localName)) {
                    return (Element) node;
                }
            }
        }
        return null;
    }

    /**
     * Gets text content of a direct child element.
     *
     * @param parent parent XML element
     * @param tagName name of child element
     *
     * @return trimmed text content or null if element not found
     */
    private String getDirectChildText(Element parent, String tagName) {
        Element element = getDirectChildElement(parent, tagName);
        return element != null ? element.getTextContent().trim() : null;
    }

    /**
     * Navigates nested elements by path and retrieves text content.
     *
     * <p>Path format: "parent/child/grandchild"
     *
     * @param root starting XML element
     * @param path slash-separated path to target element
     *
     * @return text content of target element or null if any element in path is missing
     */
    private String getNestedText(Element root, String path) {
        String[] parts = path.split("/");
        Element current = root;
        for (String part : parts) {
            current = getDirectChildElement(current, part);
            if (current == null) return null;
        }
        return current != null ? current.getTextContent().trim() : null;
    }

    /**
     * Extracts the property name from a Maven placeholder string.
     *
     * <p>Supports standard Maven placeholders in format {@code ${propertyName}}.
     * Returns {@code null} for invalid or non-placeholder input.</p>
     *
     * @param text String to parse (may be null)
     * @return Extracted property name (trimmed) or null if not a valid placeholder
     */
    private String extractPlaceholderName(String text) {
        if (text == null) return null;
        if (text.startsWith("${") && text.endsWith("}")) {
            return text.substring(2, text.length() - 1).trim();
        }
        return null;
    }


    /**
     * Checks if a string is null or empty (after trimming).
     *
     * @param str string to check
     *
     * @return true if string is null, empty, or whitespace-only
     */
    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}