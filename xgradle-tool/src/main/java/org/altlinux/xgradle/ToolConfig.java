package org.altlinux.xgradle;

import java.util.ArrayList;
import java.util.List;

public class ToolConfig {
    private List<String> excludedArtifact = new ArrayList<>();
    private boolean allowSnapshots;

    public List<String> getExcludedArtifacts() {
        return excludedArtifact;
    }

    public void setExcludedArtifacts(List<String> excludedArtifact) {
        this.excludedArtifact = excludedArtifact;
    }

    public boolean isAllowSnapshots() {
        return allowSnapshots;
    }

    public void setAllowSnapshots(boolean allowSnapshots) {
        this.allowSnapshots = allowSnapshots;
    }
}
