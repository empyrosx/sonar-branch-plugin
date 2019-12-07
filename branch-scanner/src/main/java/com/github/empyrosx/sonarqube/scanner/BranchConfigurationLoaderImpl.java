package com.github.empyrosx.sonarqube.scanner;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.scan.branch.*;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.github.empyrosx.sonarqube.scanner.ScannerSettings.*;

public class BranchConfigurationLoaderImpl implements BranchConfigurationLoader {
    public static final String LONG_LIVED_BRANCHES_REGEX = "(branch|release)-.*";

    private static final Logger LOG = Loggers.get(BranchConfigurationLoaderImpl.class);
    private static final Set<String> BRANCH_PARAMETERS = new HashSet<>(Arrays.asList(SONAR_BRANCH_NAME, SONAR_BRANCH_TARGET));
    private static final Set<String> PULL_REQUEST_PARAMETERS = new HashSet<>(Arrays.asList(SONAR_PR_KEY, SONAR_PR_BRANCH, SONAR_PR_BASE));

    public BranchConfigurationLoaderImpl() {
    }

    @Override
    public BranchConfiguration load(Map<String, String> localSettings, Supplier<Map<String, String>> settingsSupplier, ProjectBranches branches, ProjectPullRequests pullRequests) {
        boolean isPullRequestAnalysis = hasAnyPrProperty(localSettings);
        boolean isBranchAnalysis = hasAnyBranchProperty(localSettings);
        if (!isBranchAnalysis && !isPullRequestAnalysis) {
            return new DefaultBranchConfiguration();
        }

        if (branches.isEmpty()) {
            throw MessageException.of("Project was never analyzed. A regular analysis is required before a branch/pull request analysis");
        }

        if (isBranchAnalysis) {
            return createBranchConfiguration(localSettings, settingsSupplier, branches);
        }

        return createPullRequestConfiguration(localSettings, branches, pullRequests);
    }

    private static boolean hasAnyBranchProperty(Map<String, String> settings) {
        return BRANCH_PARAMETERS.stream().anyMatch(settings::containsKey);
    }

    private static boolean hasAnyPrProperty(Map<String, String> mutableSettings) {
        return PULL_REQUEST_PARAMETERS.stream()
                .anyMatch((param) -> StringUtils.trimToNull(mutableSettings.get(param)) != null);
    }

    private static BranchConfiguration createBranchConfiguration(Map<String, String> localSettings, Supplier<Map<String, String>> settingsSupplier, ProjectBranches branches) {
        if (StringUtils.trimToNull(localSettings.get(SONAR_BRANCH_NAME)) == null) {
            throw MessageException.of(String.format("Parameter '%s' is mandatory for a branch analysis", "sonar.branch.name"));
        }

        localSettings.keySet().stream()
                .filter(PULL_REQUEST_PARAMETERS::contains)
                .findAny()
                .ifPresent((param) -> {
                    throw MessageException.of(String.format("A branch analysis cannot have the pull request analysis parameter '%s'", param));
                });

        String branchName = StringUtils.trimToNull(localSettings.get(SONAR_BRANCH_NAME));
        String branchTarget = StringUtils.trimToNull(localSettings.get(SONAR_BRANCH_TARGET));
        if (branchTarget == null) {
            String defaultBranchName = branches.defaultBranchName();
            if (!branchName.equals(defaultBranchName)) {
                branchTarget = defaultBranchName;
            }
        }

        BranchInfo existingBranch = branches.get(branchName);
        if (isExistingLongBranch(existingBranch)) {
            if (existingBranch.isMain() && branchTarget != null) {
                throw MessageException.of("The main branch must not have a target");
            } else {
                return new BranchConfigurationImpl(existingBranch.type(), branchName, branchTarget, branchName, null);
            }
        } else {
            String longLivingSonarReferenceBranch;
            if (branches.get(branchTarget) != null) {
                longLivingSonarReferenceBranch = computeReferenceBranch(branchTarget, branches);
            } else {
                longLivingSonarReferenceBranch = branches.defaultBranchName();
            }

            BranchType branchType;
            if (isExistingShortBranch(existingBranch)) {
                branchType = existingBranch.type();
            } else {
                branchType = computeBranchType(settingsSupplier, branchName);
            }

            return new BranchConfigurationImpl(branchType, branchName, branchTarget, longLivingSonarReferenceBranch, null);
        }
    }

    private static boolean isExistingLongBranch(@Nullable BranchInfo existingBranch) {
        return existingBranch != null && existingBranch.type() == BranchType.LONG;
    }

    private static boolean isExistingShortBranch(@Nullable BranchInfo existingBranch) {
        return existingBranch != null && existingBranch.type() != BranchType.LONG;
    }

    private static BranchConfiguration createPullRequestConfiguration(Map<String, String> localSettings, ProjectBranches branches, ProjectPullRequests pullRequests) {
        validatePullRequestConfiguration(localSettings);
        String pullRequestKey = StringUtils.trimToNull(localSettings.get(SONAR_PR_KEY));
        String pullRequestBranch = StringUtils.trimToNull(localSettings.get(SONAR_PR_BRANCH));
        String pullRequestBase = StringUtils.trimToNull(localSettings.get(SONAR_PR_BASE));
        if (pullRequestBase == null) {
            pullRequestBase = branches.defaultBranchName();
        }

        String longLivingSonarReferenceBranch;
        if (branches.get(pullRequestBase) != null) {
            longLivingSonarReferenceBranch = computeReferenceBranch(pullRequestBase, branches);
        } else if (pullRequests.get(pullRequestBase) != null) {
            longLivingSonarReferenceBranch = computeReferenceBranchFromPullRequests(pullRequestBase, branches, pullRequests);
        } else {
            longLivingSonarReferenceBranch = branches.defaultBranchName();
        }

        return new BranchConfigurationImpl(BranchType.PULL_REQUEST, pullRequestBranch, pullRequestBase, longLivingSonarReferenceBranch, pullRequestKey);
    }

    private static void validatePullRequestConfiguration(Map<String, String> localSettings) {
        Stream.of(SONAR_PR_KEY, SONAR_PR_BRANCH)
                .filter((param) -> StringUtils.trimToNull(localSettings.get(param)) == null)
                .findAny()
                .ifPresent((param) -> {
                    throw MessageException.of(String.format("Parameter '%s' is mandatory for a pull request analysis", param));
                });
    }

    private static String computeReferenceBranch(String targetBranch, ProjectBranches branches) {
        BranchInfo target = getBranch(branches, targetBranch);
        if (target.type() == BranchType.LONG) {
            return targetBranch;
        } else {
            LOG.info("The target branch '{}' is not a long branch. Using its own target instead: '{}'", target.name(), target.branchTargetName());
            String targetOfTargetName = target.branchTargetName();
            if (targetOfTargetName == null) {
                throw MessageException.of(String.format("Illegal state: the target branch '%s' was expected to have a target", target.name()));
            } else {
                BranchInfo targetOfTarget = getBranch(branches, targetOfTargetName);
                if (targetOfTarget.type() != BranchType.LONG) {
                    throw MessageException.of(String.format("Illegal state: the target of the target branch '%s' was expected to be a long living branch", targetOfTargetName));
                } else {
                    return targetOfTarget.name();
                }
            }
        }
    }

    private static String computeReferenceBranchFromPullRequests(String pullRequestBaseName, ProjectBranches branches, ProjectPullRequests pullRequests) {
        PullRequestInfo pullRequestBase = getPullRequest(pullRequests, pullRequestBaseName);
        String resolvedBaseBranchName = pullRequestBase.getBase();
        if (resolvedBaseBranchName == null) {
            throw MessageException.of(String.format("Illegal state: the pull request '%s' was expected to have a base branch", pullRequestBase.getKey()));
        } else {
            LOG.info("The base branch '{}' is only analyzed as a pull request. Using its base instead: '{}'", pullRequestBase.getBranch(), resolvedBaseBranchName);
            BranchInfo resolvedBaseBranch = getBranch(branches, resolvedBaseBranchName);
            if (resolvedBaseBranch.type() != BranchType.LONG) {
                throw MessageException.of(String.format("Illegal state: the base '%s' of the branch '%s' was expected to be a long living branch", resolvedBaseBranchName, pullRequestBaseName));
            } else {
                return resolvedBaseBranch.name();
            }
        }
    }

    private static BranchInfo getBranch(ProjectBranches branches, String branchName) {
        BranchInfo branch = branches.get(branchName);
        if (branch == null) {
            throw MessageException.of("Branch does not exist on server: " + branchName);
        } else {
            return branch;
        }
    }

    private static PullRequestInfo getPullRequest(ProjectPullRequests pullRequests, String pullRequestBase) {
        return pullRequests.get(pullRequestBase);
    }

    private static BranchType computeBranchType(Supplier<Map<String, String>> settingsSupplier, String branchName) {
        Map<String, String> settings = settingsSupplier.get();
        String longLivedBranchesRegex = settings.get("sonar.branch.longLivedBranches.regex");
        if (longLivedBranchesRegex == null) {
            longLivedBranchesRegex = LONG_LIVED_BRANCHES_REGEX;
        }

        return branchName.matches(longLivedBranchesRegex) ? BranchType.LONG : BranchType.SHORT;
    }
}
