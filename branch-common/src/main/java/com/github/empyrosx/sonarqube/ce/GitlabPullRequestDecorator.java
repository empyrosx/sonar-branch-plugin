package com.github.empyrosx.sonarqube.ce;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.*;
import org.sonar.api.ce.posttask.Analysis;
import org.sonar.api.ce.posttask.Branch;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.Issue;
import org.sonar.api.platform.Server;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.core.issue.DefaultIssue;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.gitlab.api.http.Method.DELETE;
import static org.sonar.api.rule.Severity.*;

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

        if (!projectAnalysis.getBranch().filter(branch -> Branch.Type.PULL_REQUEST == branch.getType()).isPresent()) {
            LOG.info("Current analysis is not for a Pull Request");
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

            String checker = configuration.get("sonar.pullrequest.gitlab.checker").orElse("SonarQube");
            postStatus(api, mergeRequest, projectAnalysis, checker);

            List<DefaultIssue> openIssues = new ArrayList<>(pullRequestIssueVisitor.getIssues());

            removeOldNotes(api, mergeRequest, checker);

            for (DefaultIssue issue : openIssues) {
                if (!Issue.STATUS_CLOSED.equals(issue.status()) && !Issue.STATUS_RESOLVED.equals(issue.status()))
                    postCommitComment(api, mergeRequest, issue, checker);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not decorate Pull Request on Gitlab", ex);
        }
    }

    private void removeOldNotes(GitlabAPI api, GitlabMergeRequest mergeRequest, String checker) throws IOException {
        String discussionsUrl = GitlabProject.URL + "/" + mergeRequest.getProjectId() +
                GitlabMergeRequest.URL + "/" + mergeRequest.getIid() +
                GitlabDiscussion.URL;

        String username = api.getUser().getUsername();
        Iterator<GitlabDiscussion[]> iterator = api.retrieve().asIterator(discussionsUrl, GitlabDiscussion[].class);
        while (iterator.hasNext()) {
            GitlabDiscussion[] discussions = iterator.next();
            for (GitlabDiscussion disc : discussions) {
                for (GitlabNote note : disc.getNotes())
                    if (note.getAuthor().getUsername().equals(username) && (note.getBody().startsWith(checker + ": "))) {
                        String tailUrl = GitlabProject.URL + "/" + mergeRequest.getProjectId() +
                                GitlabMergeRequest.URL + "/" + mergeRequest.getIid() +
                                GitlabDiscussion.URL + "/" + disc.getId() +
                                GitlabNote.URL + "/" + note.getId();
                        try {
                            api.retrieve().method(DELETE).to(tailUrl, Void.class);
                        } catch (Exception e) {
                            LOG.warn("Comment %s is not deleted", tailUrl);
                        }
//                    This method expects int disc.getId() but really it String
//                    TODO: fix it in gitlab-api
//                    api.deleteDiscussionNote(mergeRequest, disc.getId(), note.getId());
                    }
            }
        }
    }

    private GitlabMergeRequest findMergeRequest(GitlabAPI api, GitlabProject project, String pullRequestBranch) throws IOException {
        List<GitlabMergeRequest> mergeRequests = api.getOpenMergeRequests(project);
        for (GitlabMergeRequest mr : mergeRequests) {
            if (mr.getIid().equals(Integer.parseInt(pullRequestBranch))) {
                return api.getMergeRequest(project, mr.getIid());
            }
        }
        throw MessageException.of(String.format("Pull request for branch %s is not found", pullRequestBranch));
    }

    private void postCommitComment(GitlabAPI api, GitlabMergeRequest mergeRequest, DefaultIssue issue, String checker) {
        String fileName = pullRequestIssueVisitor.getFileName(issue);
        try {
            List<GitlabCommit> commits = api.getCommits(mergeRequest);
            commits.sort(Comparator.comparing(GitlabCommit::getCommittedDate));

            List<String> diffs = new ArrayList<>();
            for (GitlabCommit commit : commits) {
                List<GitlabCommitDiff> commitDiffs = api.getCommitDiffs(mergeRequest.getProjectId(), commit.getId());
                for (GitlabCommitDiff diff : commitDiffs) {
                    if (diff.getNewPath().equalsIgnoreCase(fileName)) {
                        diffs.add(diff.getDiff());
                    }
                }
            }

            Integer oldLine = issue.getLine() == null ? null : DiffUtils.getBaseSourceLine(diffs, issue.getLine());

            LOG.info("Calculating base line for file: " + fileName);
            LOG.info("New line: " + issue.getLine());
            LOG.info("Old line: " + oldLine);

            String message = checker + ": " + getIcon(issue) + " " + issue.getMessage();
            api.createTextDiscussion(mergeRequest, message,
                    null,
                    mergeRequest.getBaseSha(),
                    mergeRequest.getStartSha(),
                    mergeRequest.getSha(),
                    fileName,
                    issue.getLine(),
                    fileName,
                    oldLine);
        } catch (Exception e) {
            LOG.error("Can't make comment", e);
        }
    }

    @Nonnull
    private String getIcon(DefaultIssue issue) {
        String icon;
        switch (issue.severity()) {
            case BLOCKER:
                icon = ":exclamation:";
                break;
            case CRITICAL:
                icon = ":arrow_up:";
                break;
            case MAJOR:
                icon = ":arrow_up_small:";
                break;
            case MINOR:
                icon = ":arrow_down:";
                break;
            case INFO:
                icon = ":information_source:";
                break;
            default:
                icon = "";
        }
        return icon;
    }

    private static String pluralOf(long value, String singleLabel, String multiLabel) {
        return value + " " + (1 == value ? singleLabel : multiLabel);
    }

    protected void postStatus(GitlabAPI api, GitlabMergeRequest mergeRequest, ProjectAnalysis projectAnalysis, String checker) throws IOException {

        List<DefaultIssue> openIssues = pullRequestIssueVisitor.getIssues().stream()
                .filter(issue -> !Issue.STATUS_CLOSED.equals(issue.status()) && !Issue.STATUS_RESOLVED.equals(issue.status()))
                .collect(Collectors.toList());
        Map<RuleType, Long> issueCounts = Arrays.stream(RuleType.values()).collect(Collectors.toMap(k -> k,
                k -> openIssues
                        .stream()
                        .filter(i -> k == i.type())
                        .count()));

        String state = (QualityGate.Status.OK == projectAnalysis.getQualityGate().getStatus() ? "success" : "failed");
        String NEW_LINE = "\n\n";

        String summaryComment = String.format("%s %s", state, NEW_LINE) +
                String.format("# Analysis Details %s", NEW_LINE) +
                String.format("## %s Issues %s", issueCounts.values().stream().mapToLong(l -> l).sum(), NEW_LINE) +
                String.format(" - %s %s", pluralOf(issueCounts.get(RuleType.BUG), "Bug", "Bugs"), NEW_LINE) +
                String.format(" - %s %s", pluralOf(issueCounts.get(RuleType.VULNERABILITY), "Vulnerability", "Vulnerabilities"), NEW_LINE) +
                String.format(" - %s %s", pluralOf(issueCounts.get(RuleType.SECURITY_HOTSPOT), "Security issue", "Security issues"), NEW_LINE) +
                String.format(" - %s %s", pluralOf(issueCounts.get(RuleType.CODE_SMELL), "Code Smell", "Code Smells"), NEW_LINE);


        String targetURL = String.format("%s/dashboard?id=%s&pullRequest=%s", server.getPublicRootUrl()
                , projectAnalysis.getProject().getKey()
                , projectAnalysis.getBranch().get().getName().get());
        api.createCommitStatus(mergeRequest.getProjectId(), mergeRequest.getSha().substring(0, 8), state, mergeRequest.getSourceBranch(), checker, targetURL, summaryComment);
    }

    private static String getProperty(String propertyName, Configuration configuration) {
        return configuration.get(propertyName)
                .orElseThrow(() -> new IllegalStateException(String.format("%s must be defined in the project configuration", propertyName)));
    }

//    @Override
    public String getDescription() {
        return "Pull Request Decoration";
    }

}