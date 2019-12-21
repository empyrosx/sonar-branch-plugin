package com.github.empyrosx.sonarqube.ce;

import org.sonar.ce.task.projectanalysis.container.ReportAnalysisComponentProvider;

import java.util.Arrays;
import java.util.List;

public class BranchReportAnalysisComponentProvider implements ReportAnalysisComponentProvider {

    @Override
    public List<Object> getComponents() {
        return Arrays.asList(PullRequestIssueVisitor.class, GitlabPullRequestDecorator.class, BranchLoaderDelegateImpl.class);
    }
}
