package com.github.empyrosx.sonarqube.scanner;

public class ScannerSettings {

    // branch analyze
    public static final String SONAR_BRANCH_NAME = "sonar.branch.name";
    public static final String SONAR_BRANCH_TARGET = "sonar.branch.target";

    // pull request analyze
    public static final String SONAR_PR_KEY = "sonar.pullrequest.key";
    public static final String SONAR_PR_BRANCH = "sonar.pullrequest.branch";
    public static final String SONAR_PR_BASE = "sonar.pullrequest.base";
}
