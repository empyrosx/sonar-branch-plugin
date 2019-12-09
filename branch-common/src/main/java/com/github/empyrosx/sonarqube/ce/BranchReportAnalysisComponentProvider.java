package com.github.empyrosx.sonarqube.ce;

import org.sonar.ce.task.projectanalysis.container.ReportAnalysisComponentProvider;

import java.util.Collections;
import java.util.List;

public class BranchReportAnalysisComponentProvider implements ReportAnalysisComponentProvider {

    @Override
    public List<Object> getComponents() {
        return Collections.singletonList(BranchLoaderDelegateImpl.class);
    }
}
