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
import java.util.function.Supplier;

import static com.github.empyrosx.sonarqube.scanner.ScannerSettings.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BranchConfigurationLoaderImplTest {

    private static final Map<String, String> empty = new HashMap<>();
    private static final Supplier<Map<String, String>> emptySupplier = () -> empty;
    private static final ProjectBranches emptyBranches = new ProjectBranches(new ArrayList<>());
    private static final ProjectPullRequests emptyPRs = new ProjectPullRequests(new ArrayList<>());
    private final ExpectedException expectedException = ExpectedException.none();

    private static final List<BranchInfo> branches = new ArrayList<>(Arrays.asList(
            new BranchInfo("master", BranchType.LONG, true, null),
            new BranchInfo("release-1.0", BranchType.LONG, false, "master"),
            new BranchInfo("release-1.0/feature/feature_1.2", BranchType.SHORT, false, "release-1.0"),
            new BranchInfo("release-1.0/feature/feature_1.3", BranchType.SHORT, false, null),
            new BranchInfo("release-1.0/feature/feature_1.4", BranchType.SHORT, false, "release-1.0/feature/feature_1.2")
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
    public void testBranchesAbsence() {
        Map<String, String> settings = new HashMap<>();
        settings.put(SONAR_BRANCH_NAME, "short_branch");

        setExceptionMsg("Project was never analyzed. A regular analysis is required before a branch/pull request analysis");

        loader.load(settings, emptySupplier, emptyBranches, emptyPRs);
    }

    @Test
    public void testDefaultConfiguration() {
        Map<String, String> settings = new HashMap<>();

        BranchConfiguration actual = loader.load(settings, emptySupplier, emptyBranches, emptyPRs);
        Assert.assertEquals(DefaultBranchConfiguration.class, actual.getClass());
    }

    @Test
    public void testDefaultBranchAnalyze() {
        Map<String, String> settings = new HashMap<>();
        String branchName = "master";
        settings.put(SONAR_BRANCH_NAME, branchName);

        BranchConfiguration actual = loader.load(settings, emptySupplier, projectBranches, emptyPRs);
        Assert.assertEquals(branchName, actual.branchName());
        Assert.assertEquals(branchName, actual.longLivingSonarReferenceBranch());
        assertNull(actual.targetBranchName());
    }

    @Test
    public void testDefaultBranchAnalyzeWithTarget() {
        Map<String, String> settings = new HashMap<>();
        String branchName = "master";
        settings.put(SONAR_BRANCH_NAME, branchName);
        settings.put(SONAR_BRANCH_TARGET, branchName);

        setExceptionMsg("The main branch must not have a target");

        loader.load(settings, emptySupplier, projectBranches, emptyPRs);
    }

    @Test
    public void testLongBranchAnalyze() {
        Map<String, String> settings = new HashMap<>();
        String branchName = "release-1.0";
        settings.put(SONAR_BRANCH_NAME, branchName);

        BranchConfiguration actual = loader.load(settings, emptySupplier, projectBranches, emptyPRs);
        Assert.assertEquals(branchName, actual.branchName());
        Assert.assertEquals(branchName, actual.longLivingSonarReferenceBranch());
        Assert.assertEquals("master", actual.targetBranchName());
    }

    @Test
    public void testShortBranchAnalyzeWithNullBranchName() {
        Map<String, String> settings = new HashMap<>();
        String branchName = "release-1.0/feature/feature_1.1";
        settings.put(SONAR_BRANCH_NAME, "");

        setExceptionMsg("Parameter 'sonar.branch.name' is mandatory for a branch analysis");

        BranchConfiguration actual = loader.load(settings, emptySupplier, projectBranches, emptyPRs);
        Assert.assertEquals(branchName, actual.branchName());
        Assert.assertEquals("master", actual.longLivingSonarReferenceBranch());
        Assert.assertEquals("master", actual.targetBranchName());
    }

    @Test
    public void testShortBranchAnalyzeWithDefaultTarget() {
        Map<String, String> settings = new HashMap<>();
        String branchName = "release-1.0/feature/feature_1.1";
        settings.put(SONAR_BRANCH_NAME, branchName);

        BranchConfiguration actual = loader.load(settings, emptySupplier, projectBranches, emptyPRs);
        Assert.assertEquals(branchName, actual.branchName());
        Assert.assertEquals("master", actual.longLivingSonarReferenceBranch());
        Assert.assertEquals("master", actual.targetBranchName());
    }

    @Test
    public void testShortBranchAnalyzeWithIncorrectTarget() {
        Map<String, String> settings = new HashMap<>();
        String branchName = "release-1.0/feature/feature_1.1";
        settings.put(SONAR_BRANCH_NAME, branchName);
        settings.put(SONAR_BRANCH_TARGET, "release-2.0");

        BranchConfiguration actual = loader.load(settings, emptySupplier, projectBranches, emptyPRs);
        Assert.assertEquals(branchName, actual.branchName());
        Assert.assertEquals("master", actual.longLivingSonarReferenceBranch());
        Assert.assertEquals("release-2.0", actual.targetBranchName());
    }

    @Test
    public void testFirstShortBranchAnalyze() {
        Map<String, String> settings = new HashMap<>();
        String branchName = "release-1.0/feature/feature_1.2";
        String branchTarget = "release-1.0";
        settings.put(SONAR_BRANCH_NAME, branchName);
        settings.put(SONAR_BRANCH_TARGET, branchTarget);

        BranchConfiguration actual = loader.load(settings, emptySupplier, projectBranches, emptyPRs);
        Assert.assertEquals(branchName, actual.branchName());
        Assert.assertEquals(branchTarget, actual.longLivingSonarReferenceBranch());
        Assert.assertEquals(branchTarget, actual.targetBranchName());
    }

    @Test
    public void testSecondShortBranchAnalyze() {
        Map<String, String> settings = new HashMap<>();
        String branchName = "release-2.0/feature/feature_1.2";
        String branchTarget = "release-2.0";
        settings.put(SONAR_BRANCH_NAME, branchName);
        settings.put(SONAR_BRANCH_TARGET, branchTarget);

        BranchConfiguration actual = loader.load(settings, emptySupplier, projectBranches, emptyPRs);
        Assert.assertEquals(branchName, actual.branchName());
        Assert.assertEquals("master", actual.longLivingSonarReferenceBranch());
        Assert.assertEquals(branchTarget, actual.targetBranchName());
    }

    @Test
    public void testSecondShortBranchAnalyzeWithParentTarget() {
        Map<String, String> settings = new HashMap<>();
        String branchName = "release-2.0/feature/feature_1.2";
        String branchTarget = "release-1.0/feature/feature_1.2";
        settings.put(SONAR_BRANCH_NAME, branchName);
        settings.put(SONAR_BRANCH_TARGET, branchTarget);

        BranchConfiguration actual = loader.load(settings, emptySupplier, projectBranches, emptyPRs);
        Assert.assertEquals(branchName, actual.branchName());
        Assert.assertEquals("release-1.0", actual.longLivingSonarReferenceBranch());
        Assert.assertEquals(branchTarget, actual.targetBranchName());
    }

    @Test
    public void testSecondShortBranchAnalyzeWithParentNullTarget() {
        Map<String, String> settings = new HashMap<>();
        String branchName = "release-2.0/feature/feature_1.2";
        String branchTarget = "release-1.0/feature/feature_1.3";
        settings.put(SONAR_BRANCH_NAME, branchName);
        settings.put(SONAR_BRANCH_TARGET, branchTarget);

        setExceptionMsg(String.format("Illegal state: the target branch '%s' was expected to have a target", branchTarget));

        loader.load(settings, emptySupplier, projectBranches, emptyPRs);
    }

    @Test
    public void testSecondShortBranchAnalyzeWithParentShortBranchTarget() {
        Map<String, String> settings = new HashMap<>();
        String branchName = "release-2.0/feature/feature_1.3";
        String branchTarget = "release-1.0/feature/feature_1.4";
        settings.put(SONAR_BRANCH_NAME, branchName);
        settings.put(SONAR_BRANCH_TARGET, branchTarget);

        setExceptionMsg(String.format("Illegal state: the target of the target branch '%s' was expected to be a long living branch"
                , "release-1.0/feature/feature_1.2"));

        loader.load(settings, emptySupplier, projectBranches, emptyPRs);
    }

    @Test
    public void testShortBranchAnalyzeWithPRParams() {
        Map<String, String> settings = new HashMap<>();
        String branchName = "release-1.0/feature/feature_1.1";
        settings.put(SONAR_BRANCH_NAME, branchName);
        settings.put(SONAR_PR_KEY, "1");

        setExceptionMsg(String.format("A branch analysis cannot have the pull request analysis parameter '%s'", SONAR_PR_KEY));

        loader.load(settings, emptySupplier, projectBranches, emptyPRs);
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

        BranchConfiguration actual = loader.load(settings, emptySupplier, projectBranches, emptyPRs);
        Assert.assertEquals(prKey, actual.pullRequestKey());
        Assert.assertEquals(branchName, actual.branchName());
        Assert.assertEquals(baseBranch, actual.targetBranchName());
        Assert.assertEquals(baseBranch, actual.longLivingSonarReferenceBranch());
    }

    @Test
    public void testPRAnalyzeWithoutKey() {
        Map<String, String> settings = new HashMap<>();
        settings.put(SONAR_PR_BRANCH, "release-1.0/pr-1");
        settings.put(SONAR_PR_BASE, "release-1.0");

        setExceptionMsg(String.format("Parameter '%s' is mandatory for a pull request analysis", SONAR_PR_KEY));

        loader.load(settings, emptySupplier, projectBranches, emptyPRs);
    }

    @Test
    public void testPRAnalyzeWithoutBase() {
        Map<String, String> settings = new HashMap<>();
        settings.put(SONAR_PR_KEY, "1");
        settings.put(SONAR_PR_BASE, "release-1.0");

        setExceptionMsg(String.format("Parameter '%s' is mandatory for a pull request analysis", SONAR_PR_BRANCH));

        loader.load(settings, emptySupplier, projectBranches, emptyPRs);
    }

    @Test
    public void testPRAnalyzeWithoutTarget() {
        Map<String, String> settings = new HashMap<>();
        String prKey = "1";
        settings.put(SONAR_PR_KEY, prKey);
        String branchName = "release-1.0/pr-1";
        settings.put(SONAR_PR_BRANCH, branchName);

        BranchConfiguration actual = loader.load(settings, emptySupplier, projectBranches, emptyPRs);
        Assert.assertEquals(prKey, actual.pullRequestKey());
        Assert.assertEquals(branchName, actual.branchName());
        Assert.assertEquals("master", actual.targetBranchName());
        Assert.assertEquals("master", actual.longLivingSonarReferenceBranch());
    }

    @Test
    public void testPRAnalyzeWithIncorrectTarget() {
        Map<String, String> settings = new HashMap<>();
        String prKey = "1";
        settings.put(SONAR_PR_KEY, prKey);
        String branchName = "release-1.0/pr-1";
        settings.put(SONAR_PR_BRANCH, branchName);
        settings.put(SONAR_PR_BASE, "something_that_does_not_exist");

        BranchConfiguration actual = loader.load(settings, emptySupplier, projectBranches, emptyPRs);
        Assert.assertEquals(prKey, actual.pullRequestKey());
        Assert.assertEquals(branchName, actual.branchName());
        Assert.assertEquals("something_that_does_not_exist", actual.targetBranchName());
        Assert.assertEquals("master", actual.longLivingSonarReferenceBranch());
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

        BranchConfiguration actual = loader.load(settings, emptySupplier, projectBranches, projectPRs);
        Assert.assertEquals(prKey, actual.pullRequestKey());
        Assert.assertEquals(branchName, actual.branchName());
        Assert.assertEquals(baseBranch, actual.targetBranchName());
        Assert.assertEquals("release-1.0", actual.longLivingSonarReferenceBranch());
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

        setExceptionMsg("Illegal state: the pull request '3' was expected to have a base branch");

        loader.load(settings, emptySupplier, projectBranches, projectPRs);
    }

    @Test
    public void testSecondPRAnalyzeWithPRShortBranchTarget() {
        Map<String, String> settings = new HashMap<>();
        String prKey = "2";
        String branchName = "release-1.0/pr-2";
        // we should calc right target from projectPRs
        String baseBranch = "release-1.0/pr-4";
        settings.put(SONAR_PR_KEY, prKey);
        settings.put(SONAR_PR_BRANCH, branchName);
        settings.put(SONAR_PR_BASE, baseBranch);

        setExceptionMsg("Illegal state: the base 'release-1.0/feature/feature_1.3' of the branch 'release-1.0/pr-4' was expected to be a long living branch");

        loader.load(settings, emptySupplier, projectBranches, projectPRs);
    }

}