package com.github.empyrosx.sonarqube.server;

import org.apache.commons.lang.StringUtils;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeTaskCharacteristicDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.ce.queue.BranchSupport;
import org.sonar.server.ce.queue.BranchSupportDelegate;

import java.time.Clock;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

public class BranchSupportDelegateImpl implements BranchSupportDelegate {

    private final UuidFactory uuidFactory;
    private final DbClient dbClient;
    private final Clock clock;

    public BranchSupportDelegateImpl(UuidFactory uuidFactory, DbClient dbClient, Clock clock) {
        super();
        this.uuidFactory = uuidFactory;
        this.dbClient = dbClient;
        this.clock = clock;
    }

    private BranchType getBranchType(Map<String, String> options) {
        String branchType = StringUtils.trim(options.get(CeTaskCharacteristicDto.BRANCH_TYPE_KEY));
        if (StringUtils.isNotEmpty(branchType)) {
            try {
                return BranchType.valueOf(branchType);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(String.format("Unsupported branch type '%s'", branchType), e);
            }
        }

        String pullRequest = StringUtils.trim(options.get(CeTaskCharacteristicDto.PULL_REQUEST));
        if (StringUtils.isNotEmpty(pullRequest)) {
            return BranchType.PULL_REQUEST;
        }

        throw new IllegalArgumentException(String.format("One of '%s' or '%s' options must be specified",
                CeTaskCharacteristicDto.BRANCH_TYPE_KEY,
                CeTaskCharacteristicDto.PULL_REQUEST));
    }

    @Override
    public BranchSupport.ComponentKey createComponentKey(String projectKey, Map<String, String> options) {
        BranchType branchType = getBranchType(options);
        switch (branchType) {
            case BRANCH: {
                String branch = StringUtils.trim(options.get(CeTaskCharacteristicDto.BRANCH_KEY));
                return ComponentKeyImpl.forBranch(projectKey, branch);
            }
            case PULL_REQUEST: {
                String pullRequest = StringUtils.trimToNull(options.get(CeTaskCharacteristicDto.PULL_REQUEST));
                return ComponentKeyImpl.forPullRequest(projectKey, pullRequest);
            }
            default:
                throw new IllegalArgumentException(String.format("Unsupported branch type '%s'", branchType));
        }
    }

    @Override
    public ComponentDto createBranchComponent(DbSession dbSession, BranchSupport.ComponentKey componentKey,
                                              ComponentDto mainComponentDto,
                                              BranchDto mainComponentBranchDto) {

        // TODO: Add logging

        if (!componentKey.getKey().equals(mainComponentDto.getKey())) {
            throw new IllegalStateException("Component Key and Main Component Key are not equal");
        }

        Optional<String> branchOptional = componentKey.getBranchName();
        if (branchOptional.isPresent() && branchOptional.get().equals(mainComponentBranchDto.getKey())) {
            return mainComponentDto;
        }

        String branchUuid = uuidFactory.create();

        ComponentDto result = mainComponentDto.copy();
        result.setUuid(branchUuid);
        result.setProjectUuid(branchUuid);
        result.setRootUuid(branchUuid);
        result.setUuidPath(ComponentDto.UUID_PATH_OF_ROOT);
        result.setModuleUuidPath(ComponentDto.UUID_PATH_SEPARATOR + branchUuid + ComponentDto.UUID_PATH_SEPARATOR);
        result.setMainBranchProjectUuid(mainComponentDto.uuid());
        result.setDbKey(componentKey.getDbKey());
        result.setCreatedAt(new Date(clock.millis()));
        dbClient.componentDao().insert(dbSession, result);
        return result;
    }

    static class ComponentKeyImpl extends BranchSupport.ComponentKey {

        private final String key;
        private final String dbKey;
        private final String branch;
        private final String pullRequestKey;

        private ComponentKeyImpl(String key, String dbKey, String branch, String pullRequestKey) {
            this.key = key;
            this.dbKey = dbKey;
            this.branch = branch;
            this.pullRequestKey = pullRequestKey;
        }

        public static BranchSupport.ComponentKey forBranch(String projectKey, String branch) {
            String branchKey = ComponentDto.generateBranchKey(projectKey, branch);
            return new ComponentKeyImpl(projectKey, branchKey, branch, null);
        }

        public static BranchSupport.ComponentKey forPullRequest(String projectKey, String pullRequest) {
            String pullRequestKey = ComponentDto.generatePullRequestKey(projectKey, pullRequest);
            return new ComponentKeyImpl(projectKey, pullRequestKey, null, pullRequest);
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getDbKey() {
            return dbKey;
        }

        //        @Override
        public Optional<String> getDeprecatedBranchName() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getBranchName() {
            return Optional.ofNullable(branch);
        }

        @Override
        public Optional<String> getPullRequestKey() {
            return Optional.ofNullable(pullRequestKey);
        }

        @Override
        public ComponentKeyImpl getMainBranchComponentKey() {
            if (key.equals(dbKey)) {
                return this;
            }
            return new ComponentKeyImpl(key, key, null, null);
        }
    }
}
