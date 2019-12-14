package com.github.empyrosx.sonarqube.ce;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabProject;
import org.sonar.api.ce.posttask.Analysis;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.core.issue.DefaultIssue;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GitlabPullRequestDecorator implements PostProjectAnalysisTask {

    private static final Logger LOG = Loggers.get(GitlabPullRequestDecorator.class);


    private final ConfigurationRepository configurationRepository;
    private final PullRequestIssueVisitor pullRequestIssueVisitor;
    private final Server server;

    public GitlabPullRequestDecorator(Server server, ConfigurationRepository configurationRepository,
                                      PullRequestIssueVisitor pullRequestIssueVisitor) {
        super();
        this.configurationRepository = configurationRepository;
        this.server = server;
        this.pullRequestIssueVisitor = pullRequestIssueVisitor;
    }

    @Override
    public void finished(@Nonnull ProjectAnalysis projectAnalysis) {
        Optional<Analysis> optionalAnalysis = projectAnalysis.getAnalysis();
        if (!optionalAnalysis.isPresent()) {
            LOG.warn("There are no analysis results");
            return;
        }

        Analysis analysis = optionalAnalysis.get();

        Optional<String> revision = analysis.getRevision();
        if (!revision.isPresent()) {
            LOG.warn("There are no commit details. Check the project is committed to Git");
            return;
        }

        try {
            Configuration configuration = configurationRepository.getConfiguration();
            final String url = getProperty("sonar.pullrequest.gitlab.url", configuration);
            final String token = getProperty("sonar.pullrequest.gitlab.token", configuration);
            final String projectId = getProperty("sonar.pullrequest.gitlab.project", configuration);
            final String pullRequestBranch = projectAnalysis.getBranch().get().getName().get();

            GitlabAPI api = GitlabAPI.connect(url, token);

            GitlabProject project = api.getProject(projectId);
            GitlabMergeRequest mergeRequest = findMergeRequest(api, project, pullRequestBranch);

            postStatus(api, mergeRequest, projectAnalysis, revision.get());

            List<DefaultIssue> openIssues = pullRequestIssueVisitor.getIssues().stream()
                    .filter(DefaultIssue::isNew)
                    .collect(Collectors.toList());

            for (DefaultIssue issue : openIssues) {
                postCommitComment(api, mergeRequest, issue);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not decorate Pull Request on Gitlab", ex);
        }

    }

    private GitlabMergeRequest findMergeRequest(GitlabAPI api, GitlabProject project, String pullRequestBranch) throws IOException {
        List<GitlabMergeRequest> mergeRequests = api.getOpenMergeRequests(project);
        for (GitlabMergeRequest mr : mergeRequests) {
            if (mr.getSourceBranch().equals(pullRequestBranch)) {
                return mr;
            }
        }
        throw MessageException.of(String.format("Pull request for branch %s is not found", pullRequestBranch));
    }

    private void postCommitComment(GitlabAPI api, GitlabMergeRequest mergeRequest, DefaultIssue issue) throws IOException {
        String fileName = pullRequestIssueVisitor.getFileName(issue);
        api.createTextDiscussion(mergeRequest, issue.getMessage(),
                null,
                mergeRequest.getBaseSha(),
                mergeRequest.getStartSha(),
                mergeRequest.getSha(),
                fileName,
                issue.getLine(),
                null,
                null);
    }

    protected void postStatus(GitlabAPI api, GitlabMergeRequest mergeRequest, ProjectAnalysis projectAnalysis, String revision) throws IOException {
        String targetURL = String.format("%s/dashboard?id=%s&pullRequest=%s", server.getPublicRootUrl()
                , projectAnalysis.getProject().getKey()
                , projectAnalysis.getBranch().get().getName().get());
        String state = (QualityGate.Status.OK == projectAnalysis.getQualityGate().getStatus() ? "Passed" : "Failed");
        api.createCommitStatus(mergeRequest.getProjectId(), revision, state, mergeRequest.getSourceBranch(), "SonarQube", targetURL, "SonarQube status");
    }

    private static String getProperty(String propertyName, Configuration configuration) {
        return configuration.get(propertyName)
                .orElseThrow(() -> new IllegalStateException(String.format("%s must be defined in the project configuration", propertyName)));
    }
}