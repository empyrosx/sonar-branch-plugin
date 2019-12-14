package com.github.empyrosx.sonarqube.ce;

import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.issue.IssueVisitor;
import org.sonar.core.issue.DefaultIssue;

import java.util.*;

public class PullRequestIssueVisitor extends IssueVisitor {

    private final List<DefaultIssue> issues = new ArrayList<>();

    private final Map<DefaultIssue, String> fileIssues = new HashMap<>();

    @Override
    public void onIssue(Component component, DefaultIssue defaultIssue) {
        if (Component.Type.FILE.equals(component.getType())) {
            Optional<String> scmPath = component.getReportAttributes().getScmPath();
            scmPath.ifPresent(filePath -> fileIssues.put(defaultIssue, filePath));
        }
        issues.add(defaultIssue);
    }

    public List<DefaultIssue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    public String getFileName(DefaultIssue issue) {
        return fileIssues.get(issue);
    }

}