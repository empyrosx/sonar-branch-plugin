package com.github.empyrosx.sonarqube.scanner;

import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonar.scanner.protocol.GsonHelper;
import org.sonar.scanner.scan.branch.BranchInfo;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.branch.ProjectBranches;
import org.sonar.scanner.scan.branch.ProjectBranchesLoader;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsResponse;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectBranchesLoaderImpl implements ProjectBranchesLoader {
    private static final Logger LOG = Loggers.get(ProjectBranchesLoaderImpl.class);
    private static final String PROJECT_BRANCHES_URL = "/api/project_branches/list";
    private final ScannerWsClient wsClient;

    public ProjectBranchesLoaderImpl(ScannerWsClient wsClient) {
        this.wsClient = wsClient;
    }

    public ProjectBranches load(@Nullable String projectKey) {
        return new ProjectBranches(this.loadBranchInfos(projectKey));
    }

    private List<BranchInfo> loadBranchInfos(String projectKey) {
        GetRequest request = new GetRequest(getUrl(projectKey));

        try (WsResponse response = this.wsClient.call(request)) {
            return parseResponse(response);
        } catch (IOException e) {
            throw MessageException.of("Could not load branches from server", e);
        } catch (HttpException e) {
            if (404 == e.code()) {
                return Collections.emptyList();
            } else {
                throw MessageException.of("Could not load branches from server", e);
            }
        }
    }

    private static String getUrl(String projectKey) {
        return PROJECT_BRANCHES_URL + "?project=" + Utils.encodeForUrl(projectKey);
    }

    private static List<BranchInfo> parseResponse(WsResponse response) throws IOException {
        try (Reader reader = response.contentReader()) {
            WsProjectBranchesResponse branchesResponse = GsonHelper.create().fromJson(reader, WsProjectBranchesResponse.class);
            return branchesResponse.branches.stream()
                    .map((data) -> new BranchInfo(data.name, BranchType.valueOf(data.type), data.isMain, data.mergeBranch))
                    .collect(Collectors.toList());
        }
    }

    private static class WsProjectBranchesResponse {
        private List<WsProjectBranch> branches = new ArrayList<>();
    }

    private static class WsProjectBranch {
        private String name;
        private String type;
        private boolean isMain;
        private String mergeBranch;
    }
}
