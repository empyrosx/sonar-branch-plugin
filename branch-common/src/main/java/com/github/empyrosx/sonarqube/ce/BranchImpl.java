package com.github.empyrosx.sonarqube.ce;

import org.apache.logging.log4j.util.Strings;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.core.component.ComponentKeys;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;

import javax.annotation.Nullable;
import java.util.Objects;

public class BranchImpl implements Branch {
    private final BranchType branchType;
    private final boolean isMain;
    @Nullable
    private final String pullRequestKey;
    @Nullable
    private final String targetBranchName;
    @Nullable
    private final String mergeBranchUuid;
    private final String name;

    BranchImpl(BranchType branchType, boolean isMain, String branchName) {
        this(branchType, isMain, branchName, null, null, null);
    }

    BranchImpl(BranchType branchType, boolean isMain, String branchName, @Nullable String mergeBranchUuid, @Nullable String var5) {
        this(branchType, isMain, branchName, mergeBranchUuid, var5, null);
    }

    BranchImpl(BranchType branchType, boolean isMain, String branchName, @Nullable String mergeBranchUuid, @Nullable String var5, @Nullable String pullRequestKey) {
        this.branchType = branchType;
        this.isMain = isMain;
        this.name = Objects.requireNonNull(branchName, "Branch name must be set");
        this.mergeBranchUuid = mergeBranchUuid;
        this.pullRequestKey = pullRequestKey;
        this.targetBranchName = var5;
    }

    public BranchType getType() {
        return this.branchType;
    }

    public String getMergeBranchUuid() {
        if (this.isMain) {
            throw new IllegalStateException("Invalid for master branch");
        } else {
            return this.mergeBranchUuid;
        }
    }

//    @Override
    public String getReferenceBranchUuid() {
        return mergeBranchUuid;
    }

    public boolean isMain() {
        return this.isMain;
    }

    public boolean isLegacyFeature() {
        return false;
    }

    public String getName() {
        return this.name;
    }

    public boolean supportsCrossProjectCpd() {
        return this.isMain;
    }

    public String getPullRequestKey() {
        if (this.branchType != BranchType.PULL_REQUEST) {
            throw new IllegalStateException("Only a branch of type PULL_REQUEST can have a pull request id.");
        } else {
            return this.pullRequestKey;
        }
    }

    public String getTargetBranchName() {
        return this.targetBranchName;
    }

    public String generateKey(String projectKey, @Nullable String fileOrDirPath) {
        String effectiveKey;
        if (null == fileOrDirPath) {
            effectiveKey = projectKey;
        } else {
            effectiveKey = ComponentKeys.createEffectiveKey(projectKey, Strings.trimToNull(fileOrDirPath));
        }

        if (isMain) {
            return effectiveKey;
        } else if (BranchType.PULL_REQUEST == branchType) {
            return ComponentDto.generatePullRequestKey(effectiveKey, Objects.requireNonNull(this.pullRequestKey, "pullRequestKey cannot be null"));
        } else {
            return ComponentDto.generateBranchKey(effectiveKey, name);
        }
    }
}
