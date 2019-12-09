package com.github.empyrosx.sonarqube.ce;

import org.sonar.api.SonarQubeSide;
import org.sonar.core.extension.CoreExtension;

/**
 * Implements branch feature.
 */
public class BranchCoreExtension implements CoreExtension {

    @Override
    public String getName() {
        return "branch-ce";
    }

    @Override
    public void load(Context context) {
        if (SonarQubeSide.COMPUTE_ENGINE == context.getRuntime().getSonarQubeSide()) {
            context.addExtensions(BranchReportAnalysisComponentProvider.class, BranchEditionProvider.class);
        }
    }
}
