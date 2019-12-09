package com.github.empyrosx.sonarqube.scanner;

import org.apache.commons.lang.StringUtils;
import org.sonar.core.config.ScannerProperties;
import org.sonar.scanner.bootstrap.GlobalConfiguration;
import org.sonar.scanner.scan.branch.BranchParamsValidator;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Special validation for branch analyze.
 */
public class BranchParamsValidatorImpl implements BranchParamsValidator {
    private static final int MAX_BRANCH_LENGTH = 255;
    private final GlobalConfiguration globalConfiguration;

    public BranchParamsValidatorImpl(GlobalConfiguration globalConfiguration) {
        this.globalConfiguration = globalConfiguration;
    }

    // before SonarQube 8.0
    public void validate(List<String> validationMessages, @Nullable String deprecatedBranchName) {
        String branchName = this.globalConfiguration.get(ScannerProperties.BRANCH_NAME).orElse(null);
        if (StringUtils.isNotEmpty(deprecatedBranchName) && StringUtils.isNotEmpty(branchName)) {
            validationMessages.add(String.format("The property \"%s\" must not be used together with the deprecated \"sonar.branch\"", "sonar.branch.name"));
        }

        String branchTarget = this.globalConfiguration.get(ScannerProperties.BRANCH_TARGET).orElse(null);
        if (StringUtils.isNotEmpty(deprecatedBranchName) && StringUtils.isNotEmpty(branchTarget)) {
            validationMessages.add(String.format("The property \"%s\" must not be used together with the deprecated \"sonar.branch\"", "sonar.branch.target"));
        }
    }

    // since SonarQube 8.0
    public void validate(List<String> validationMessages) {
        String branchName = this.globalConfiguration.get(ScannerProperties.BRANCH_NAME).orElse(null);
        if (StringUtils.isNotEmpty(branchName) && branchName.length() > MAX_BRANCH_LENGTH) {
            validationMessages.add(String.format("'%s' is not a valid branch name. Max length is %d characters.", branchName, 255));
        }
    }
}
