package com.github.empyrosx.sonarqube.scanner;

import org.sonar.api.CoreProperties;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
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

        context.addExtensions(
                PropertyDefinition.builder(CoreProperties.LONG_LIVED_BRANCHES_REGEX)
                        .onQualifiers(Qualifiers.PROJECT)
                        .category(CoreProperties.CATEGORY_GENERAL)
                        .subCategory(CoreProperties.SUBCATEGORY_BRANCHES)
                        .defaultValue(BranchConfigurationLoaderImpl.LONG_LIVED_BRANCHES_REGEX)
                        .build()
        );
    }
}