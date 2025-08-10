package org.altlinux.gradlePlugin.core.processors;

import org.altlinux.gradlePlugin.core.managers.ScopeManager;
import org.altlinux.gradlePlugin.core.managers.TransitiveDependencyManager;
import org.altlinux.gradlePlugin.model.MavenCoordinate;
import org.altlinux.gradlePlugin.services.FileSystemArtifactVerifier;
import org.altlinux.gradlePlugin.services.PomFinder;
import org.gradle.api.logging.Logger;
import java.util.*;

public class TransitiveProcessor {
    private final TransitiveDependencyManager transitiveManager;
    private final ScopeManager scopeManager = new ScopeManager();
    private final Set<String> mainDependencies = new HashSet<>();
    private final Set<String> testDependencies = new HashSet<>();
    private final Set<String> testContextDependencies;

    public TransitiveProcessor(PomFinder pomFinder, Logger logger, Set<String> testContextDependencies) {
        this.transitiveManager = new TransitiveDependencyManager(
                pomFinder, new FileSystemArtifactVerifier(), logger
        );
        this.testContextDependencies = testContextDependencies;
    }

    public void process(Map<String, MavenCoordinate> systemArtifacts) {
        for (MavenCoordinate coord : systemArtifacts.values()) {
            if (testContextDependencies.contains(coord.getGroupId() + ":" + coord.getArtifactId())) {
                coord.setTestContext(true);
            }
        }

        transitiveManager.processTransitiveDependencies(systemArtifacts);

        for (Map.Entry<String, MavenCoordinate> entry : systemArtifacts.entrySet()) {
            MavenCoordinate coord = entry.getValue();
            if (coord.isTestContext()) {
                testDependencies.add(entry.getKey());
            } else {
                mainDependencies.add(entry.getKey());
            }
        }
    }

    public Set<String> getMainDependencies() {
        return mainDependencies;
    }

    public Set<String> getTestDependencies() {
        return testDependencies;
    }

    public ScopeManager getScopeManager() {
        return scopeManager;
    }

    public Set<String> getSkippedDependencies() {
        return transitiveManager.getSkippedDependencies();
    }
}