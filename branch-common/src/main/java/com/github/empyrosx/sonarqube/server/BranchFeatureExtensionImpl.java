package com.github.empyrosx.sonarqube.server;

import org.sonar.server.branch.BranchFeatureExtension;

/**
 * Enables branch feature support.
 */
public class BranchFeatureExtensionImpl implements BranchFeatureExtension {

    @Override
    public boolean isEnabled() {
        return true;
    }
}
