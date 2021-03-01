package com.github.empyrosx.sonarqube.ce;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.ce.task.projectanalysis.analysis.MutableAnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.BranchLoaderDelegate;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

import static java.lang.String.format;

public class BranchLoaderDelegateImpl implements BranchLoaderDelegate {
    private final DbClient dbClient;
    private final MutableAnalysisMetadataHolder metadataHolder;

    public BranchLoaderDelegateImpl(DbClient dbClient, MutableAnalysisMetadataHolder metadataHolder) {
        this.dbClient = dbClient;
        this.metadataHolder = metadataHolder;
    }

    public void load(@Nonnull ScannerReport.Metadata metadata) {
        BranchImpl branch = createBranch(metadata);
        this.metadataHolder.setBranch(branch);
        this.metadataHolder.setPullRequestKey(metadata.getPullRequestKey());
    }

    private BranchImpl createBranch(ScannerReport.Metadata metadata) {
        BranchImpl result;
        String branchName = StringUtils.trimToNull(metadata.getBranchName());
        if (branchName == null) {
            Optional<BranchDto> mainBranchDto = this.findBranchByUuid(this.metadataHolder.getProject().getUuid());
            BranchDto branchDto = mainBranchDto.get();
            result = mainBranchDto
                    .map((value) -> new BranchImpl(branchDto.getBranchType(), branchDto.isMain(), branchDto.getKey()))
                    .orElseGet(() -> new BranchImpl(branchDto.getBranchType(), branchDto.isMain(), "master"));
        } else {
            String targetBranch = StringUtils.trimToNull(metadata.getReferenceBranchName());
            String targetBranchName = StringUtils.trimToNull(metadata.getTargetBranchName());
            BranchType branchType = detectBranchType(metadata.getBranchType());
            Project project = this.metadataHolder.getProject();
            if (branchType == BranchType.PULL_REQUEST) {
                result = this.createPullRequest(StringUtils.trimToNull(metadata.getPullRequestKey()), targetBranchName, branchName, targetBranch);
            } else {
                result = createBranch(branchName, targetBranch, targetBranchName, branchType, project);
            }
        }
        return result;
    }

    private static BranchType detectBranchType(ScannerReport.Metadata.BranchType branchType) {
        switch (branchType) {
            case BRANCH:
                return BranchType.valueOf("BRANCH");
            case PULL_REQUEST:
                return BranchType.PULL_REQUEST;
            case UNSET:
            default:
                throw new IllegalStateException("Invalid branch type: " + branchType);
        }
    }

    private BranchImpl createBranch(String branchName, String targetBranch, String targetBranchName, BranchType branchType, Project project) {
        boolean isMainBranch = this.findBranchByKey(project.getUuid(), branchName)
                .map(BranchDto::isMain)
                .orElse(false);

        String targetUuid = getTargetBranchUuid(targetBranch, project);
        return new BranchImpl(branchType, isMainBranch, branchName, targetUuid, targetBranchName);
    }

    private BranchImpl createPullRequest(String pullRequestKey, String targetBranchName, String branchName, @Nullable String targetBranch) {
        Project project = this.metadataHolder.getProject();
        String targetUuid = getTargetBranchUuid(targetBranch, project);
        return new BranchImpl(BranchType.PULL_REQUEST, false, branchName, targetUuid, targetBranchName, pullRequestKey);
    }

    private String getTargetBranchUuid(String targetBranch, Project project) {
        String targetUuid;
        if (targetBranch == null) {
            targetUuid = project.getUuid();
        } else {
            BranchDto dto = this.findBranchByKey(project.getUuid(), targetBranch)
                    .orElseThrow(() -> new IllegalStateException(format("Merge branch '%s' does not exist", targetBranch)));
            if (dto.getBranchType() == BranchType.PULL_REQUEST) {
                throw MessageException.of(format("Invalid merge branch '%s': it must be a long branch but it is '%s'", targetBranch, dto.getBranchType()));
            }
            targetUuid = dto.getUuid();
        }
        return targetUuid;
    }

    private Optional<BranchDto> findBranchByKey(String projectUuid, String key) {
        try (DbSession dbSession = dbClient.openSession(false)) {
            return dbClient.branchDao().selectByBranchKey(dbSession, projectUuid, key);
        }
    }

    private Optional<BranchDto> findBranchByUuid(String projectUuid) {
        try (DbSession dbSession = dbClient.openSession(false)) {
            return dbClient.branchDao().selectByUuid(dbSession, projectUuid);
        }
    }
}
