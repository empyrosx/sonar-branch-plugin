package com.github.empyrosx.sonarqube.scanner;

import org.sonar.api.SonarQubeSide;
import org.sonar.core.extension.CoreExtension;

/**
 * Scanner core extension for branch analyze.
 */
public class ScannerCoreExtension implements CoreExtension {

    public String getName() {
        return "branch-scanner";
    }

    public void load(Context context) {
        if (context.getRuntime().getSonarQubeSide() == SonarQubeSide.SCANNER) {
            context.addExtensions(BranchParamsValidatorImpl.class, BranchConfigurationLoaderImpl.class,
                    ProjectBranchesLoaderImpl.class, ProjectPullRequestsLoaderImpl.class);
        }
    }
}