package com.github.empyrosx.sonarqube.scanner;

import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchType;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class BranchConfigurationImpl implements BranchConfiguration {
    private final BranchType branchType;
    private final String branchName;
    @Nullable
    private final String referenceBranchName;
    @Nullable
    private final String targetBranchName;
    @Nullable
    private final String pullRequestKey;

    BranchConfigurationImpl(BranchType branchType,
                            String branchName,
                            @Nullable String referenceBranchName,
                            @Nullable String targetBranchName,
                            @Nullable String pullRequestKey) {
        this.branchType = branchType;
        this.branchName = branchName;
        this.referenceBranchName = referenceBranchName;
        this.targetBranchName = targetBranchName;
        this.pullRequestKey = pullRequestKey;
    }

    public BranchType branchType() {
        return this.branchType;
    }

    public String branchName() {
        return this.branchName;
    }

    @CheckForNull
    public String targetBranchName() {
        return this.targetBranchName;
    }

    @CheckForNull
    public String referenceBranchName() {
        return this.referenceBranchName;
    }

    public String pullRequestKey() {
        if (this.branchType != BranchType.PULL_REQUEST) {
            throw new IllegalStateException("Only a branch of type PULL_REQUEST can have a pull request key.");
        } else {
            return this.pullRequestKey;
        }
    }
}
