package com.github.empyrosx.sonarqube.server;

import org.sonar.api.SonarQubeSide;
import org.sonar.core.extension.CoreExtension;

/**
 * Implements branch feature.
 */
public class BranchCoreExtension implements CoreExtension {

    @Override
    public String getName() {
        return "branch-server";
    }

    @Override
    public void load(Context context) {
        if (SonarQubeSide.SERVER == context.getRuntime().getSonarQubeSide()) {
            context.addExtensions(BranchFeatureExtensionImpl.class, BranchSupportDelegateImpl.class);
        }
    }
}
