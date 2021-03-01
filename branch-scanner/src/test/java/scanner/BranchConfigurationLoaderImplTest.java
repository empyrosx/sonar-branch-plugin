package scanner;

import com.github.empyrosx.sonarqube.scanner.BranchConfigurationLoaderImpl;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.scan.branch.*;

import java.util.*;

import static com.github.empyrosx.sonarqube.scanner.ScannerSettings.*;
import static org.junit.Assert.assertNull;

public class BranchConfigurationLoaderImplTest {

    private static final ProjectBranches emptyBranches = new ProjectBranches(new ArrayList<>());
    private static final ProjectPullRequests emptyPRs = new ProjectPullRequests(new ArrayList<>());
    private final ExpectedException expectedException = ExpectedException.none();

    private static final List<BranchInfo> branches = new ArrayList<>(Arrays.asList(
            new BranchInfo("master", BranchType.BRANCH, true, null),
            new BranchInfo("release-1.0", BranchType.BRANCH, false, "master"),
            new BranchInfo("release-1.0/feature/feature_1.2", BranchType.BRANCH, false, "release-1.0"),
            new BranchInfo("release-1.0/feature/feature_1.3", BranchType.BRANCH, false, null),
            new BranchInfo("release-1.0/feature/feature_1.4", BranchType.BRANCH, false, "release-1.0/feature/feature_1.2")
    ));
    private static final ProjectBranches projectBranches = new ProjectBranches(branches);


    private static final List<PullRequestInfo> pullRequests = new ArrayList<>(Arrays.asList(
            new PullRequestInfo("2", "release-1.0/pr-2", "release-1.0", 0),
            new PullRequestInfo("3", "release-1.0/pr-3", null, 0),
            new PullRequestInfo("4", "release-1.0/pr-4", "release-1.0/feature/feature_1.3", 0)
    ));

    private static final ProjectPullRequests projectPRs = new ProjectPullRequests(pullRequests);
    private BranchConfigurationLoader loader;

    @Before
    public void setUp() {
        loader = new BranchConfigurationLoaderImpl();
    }

    @Rule
    public ExpectedException expectedException() {
        return expectedException;
    }

    private void setExceptionMsg(final String message) {
        expectedException.expect(MessageException.class);
        expectedException.expectMessage(IsEqual.equalTo(message));
    }

    @Test
    public void testDefaultConfiguration() {
        Map<String, String> settings = new HashMap<>();

        BranchConfiguration actual = loader.load(settings, emptyBranches, emptyPRs);
        Assert.assertEquals(DefaultBranchConfiguration.class, actual.getClass());
    }

    @Test
    public void testDefaultBranchAnalyze() {
        Map<String, String> settings = new HashMap<>();
        String branchName = "master";
        settings.put(SONAR_BRANCH_NAME, branchName);

        BranchConfiguration actual = loader.load(settings, projectBranches, emptyPRs);
        Assert.assertEquals(branchName, actual.branchName());
        Assert.assertEquals(branchName, actual.referenceBranchName());
        assertNull(actual.targetBranchName());
    }

    @Test
    public void testLongBranchAnalyze() {
        Map<String, String> settings = new HashMap<>();
        String branchName = "release-1.0";
        settings.put(SONAR_BRANCH_NAME, branchName);

        BranchConfiguration actual = loader.load(settings, projectBranches, emptyPRs);
        Assert.assertEquals(branchName, actual.branchName());
        Assert.assertEquals(branchName, actual.referenceBranchName());
        assertNull(actual.targetBranchName());
    }

    @Test
    public void testPRAnalyze() {
        Map<String, String> settings = new HashMap<>();
        String prKey = "1";
        settings.put(SONAR_PR_KEY, prKey);
        String branchName = "release-1.0/pr-1";
        settings.put(SONAR_PR_BRANCH, branchName);
        String baseBranch = "release-1.0";
        settings.put(SONAR_PR_BASE, baseBranch);

        BranchConfiguration actual = loader.load(settings, projectBranches, emptyPRs);
        Assert.assertEquals(prKey, actual.pullRequestKey());
        Assert.assertEquals(branchName, actual.branchName());
        Assert.assertEquals(baseBranch, actual.targetBranchName());
        Assert.assertEquals(baseBranch, actual.referenceBranchName());
    }

    @Test
    public void testPRAnalyzeWithoutTarget() {
        Map<String, String> settings = new HashMap<>();
        String prKey = "1";
        settings.put(SONAR_PR_KEY, prKey);
        String branchName = "release-1.0/pr-1";
        settings.put(SONAR_PR_BRANCH, branchName);

        BranchConfiguration actual = loader.load(settings, projectBranches, emptyPRs);
        Assert.assertEquals(prKey, actual.pullRequestKey());
        Assert.assertEquals(branchName, actual.branchName());
        Assert.assertEquals("master", actual.targetBranchName());
        Assert.assertEquals("master", actual.referenceBranchName());
    }

    @Test
    public void testPRAnalyzeWithIncorrectTarget() {
        Map<String, String> settings = new HashMap<>();
        String prKey = "1";
        settings.put(SONAR_PR_KEY, prKey);
        String branchName = "release-1.0/pr-1";
        settings.put(SONAR_PR_BRANCH, branchName);
        settings.put(SONAR_PR_BASE, "something_that_does_not_exist");

        BranchConfiguration actual = loader.load(settings, projectBranches, emptyPRs);
        Assert.assertEquals(prKey, actual.pullRequestKey());
        Assert.assertEquals(branchName, actual.branchName());
        Assert.assertEquals("something_that_does_not_exist", actual.targetBranchName());
        Assert.assertEquals("master", actual.referenceBranchName());
    }

    @Test
    public void testSecondPRAnalyze() {
        Map<String, String> settings = new HashMap<>();
        String prKey = "2";
        String branchName = "release-1.0/pr-2";
        // we should calc right target from projectPRs
        String baseBranch = "release-1.0/pr-2";
        settings.put(SONAR_PR_KEY, prKey);
        settings.put(SONAR_PR_BRANCH, branchName);
        settings.put(SONAR_PR_BASE, baseBranch);

        BranchConfiguration actual = loader.load(settings, projectBranches, projectPRs);
        Assert.assertEquals(prKey, actual.pullRequestKey());
        Assert.assertEquals(branchName, actual.branchName());
        Assert.assertEquals(baseBranch, actual.targetBranchName());
        Assert.assertEquals("master", actual.referenceBranchName());
    }

    @Test
    public void testSecondPRAnalyzeWithPRNullTarget() {
        Map<String, String> settings = new HashMap<>();
        String prKey = "2";
        String branchName = "release-1.0/pr-2";
        // we should calc right target from projectPRs
        String baseBranch = "release-1.0/pr-3";
        settings.put(SONAR_PR_KEY, prKey);
        settings.put(SONAR_PR_BRANCH, branchName);
        settings.put(SONAR_PR_BASE, baseBranch);

        BranchConfiguration actual = loader.load(settings, projectBranches, projectPRs);
        Assert.assertEquals(prKey, actual.pullRequestKey());
        Assert.assertEquals(branchName, actual.branchName());
        Assert.assertEquals(baseBranch, actual.targetBranchName());
        Assert.assertEquals("master", actual.referenceBranchName());
    }
}