package com.github.empyrosx.sonarqube.ce;

import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.core.config.PurgeConstants;
import org.sonar.core.extension.CoreExtension;

/**
 * Implements branch feature.
 */
public class BranchCoreExtension implements CoreExtension {

    private static final String PULL_REQUEST_CATEGORY_LABEL = "Pull Request";
    private static final String GENERAL = "General";
    private static final String GITLAB_INTEGRATION_SUBCATEGORY_LABEL = "Integration With Gitlab";

    @Override
    public String getName() {
        return "Pull requests";
    }

    @Override
    public void load(Context context) {
        if (SonarQubeSide.COMPUTE_ENGINE == context.getRuntime().getSonarQubeSide()) {
            context.addExtensions(BranchReportAnalysisComponentProvider.class, BranchEditionProvider.class);
        }

        context.addExtensions(
                PropertyDefinition.builder(PurgeConstants.DAYS_BEFORE_DELETING_INACTIVE_SHORT_LIVING_BRANCHES)
                        .name("Number of days before purging inactive short living branches")
                        .description("Short living branches are permanently deleted when there are no analysis for the configured number of days.")
                        .category(CoreProperties.CATEGORY_GENERAL)
                        .subCategory(CoreProperties.SUBCATEGORY_DATABASE_CLEANER)
                        .defaultValue("30")
                        .type(PropertyType.INTEGER)
                        .build(),
                PropertyDefinition.builder(CoreProperties.LONG_LIVED_BRANCHES_REGEX)
                        .onQualifiers(Qualifiers.PROJECT)
                        .category(CoreProperties.CATEGORY_GENERAL)
                        .subCategory(CoreProperties.SUBCATEGORY_BRANCHES)
                        .defaultValue("(branch|release)-.*")
                        .build(),
                PropertyDefinition.builder("sonar.pullrequest.provider")
                        .onlyOnQualifiers(Qualifiers.PROJECT)
                        .subCategory(PULL_REQUEST_CATEGORY_LABEL)
                        .subCategory(GENERAL)
                        .name("Provider")
                        .type(PropertyType.SINGLE_SELECT_LIST)
                        .options("GitlabServer")
                        .build(),
                PropertyDefinition.builder("sonar.pullrequest.gitlab.url")
                        .onQualifiers(Qualifiers.PROJECT)
                        .subCategory(PULL_REQUEST_CATEGORY_LABEL)
                        .subCategory(GITLAB_INTEGRATION_SUBCATEGORY_LABEL)
                        .name("URL for Gitlab (Server or Cloud) instance")
                        .description("Example: https://ci-server.local/gitlab")
                        .type(PropertyType.STRING)
                        .defaultValue("https://gitlab.com")
                        .build(),
                PropertyDefinition.builder("sonar.pullrequest.gitlab.token")
                        .onQualifiers(Qualifiers.PROJECT)
                        .subCategory(PULL_REQUEST_CATEGORY_LABEL)
                        .subCategory(GITLAB_INTEGRATION_SUBCATEGORY_LABEL)
                        .name("The token for the user to comment to the PR on Gitlab (Server or Cloud) instance")
                        .description("Token used for authentication and commenting to your Gitlab instance")
                        .type(PropertyType.STRING)
                        .build(),
                PropertyDefinition.builder("sonar.pullrequest.gitlab.project")
                        .onQualifiers(Qualifiers.PROJECT)
                        .subCategory(PULL_REQUEST_CATEGORY_LABEL)
                        .subCategory(GITLAB_INTEGRATION_SUBCATEGORY_LABEL)
                        .name("Repository project for the Gitlab (Server or Cloud) instance")
                        .description("The repository project can be user/repo or just project ID")
                        .type(PropertyType.STRING)
                        .build()
        );
    }
}
