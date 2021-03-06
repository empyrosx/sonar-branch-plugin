package com.github.empyrosx.sonarqube;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.codec.binary.Base64;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.scanner.protocol.GsonHelper;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.com.google.common.net.HttpHeaders;
import org.testcontainers.utility.MountableFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class IntegrationTests {

    private String sonarImage;

    @Rule
    public Network network = Network.newNetwork();

    private Logger logger = LoggerFactory.getLogger(IntegrationTests.class.getName());

    private List<String> tasks = new ArrayList<>();
    private Consumer<OutputFrame> consumer = (outputFrame) -> {
        String line = outputFrame.getUtf8String();
        String taskMarker = "Executed task";
        if (line.contains(taskMarker)) {
            tasks.add(line);
        }
    };
    private GenericContainer sonarQube;

    public IntegrationTests(String sonarImage) {
        this.sonarImage = sonarImage;
    }

    @Rule
    public GenericContainer getSonarQube() {
        this.sonarQube = new GenericContainer(sonarImage)
                .withCopyFileToContainer(MountableFile.forHostPath("branch-scanner/build/libs/branch-scanner.jar"), "/opt/sonarqube/lib/scanner/")
                .withCopyFileToContainer(MountableFile.forHostPath("branch-common/build/libs/branch-common.jar"), "/opt/sonarqube/lib/common/")
                .withExposedPorts(9000)
                .withLogConsumer(consumer)
                .withLogConsumer(new Slf4jLogConsumer(logger))
                .withNetwork(network)
                .withNetworkAliases("sonarqube")
                .waitingFor(Wait.forLogMessage(".*SonarQube is up.*", 1));
        return this.sonarQube;
    }

    GenericContainer createScanner(String resourcePath) {
        return new GenericContainer("newtmitch/sonar-scanner:alpine")
                .withClasspathResourceMapping(resourcePath, "/usr/src", BindMode.READ_WRITE)
                .withLogConsumer(new Slf4jLogConsumer(logger))
                .withNetwork(network)
                .waitingFor(Wait.forLogMessage(".*EXECUTION SUCCESS.*", 1));
    }

    @Parameters(name = "{0}")
    public static Object[] data() {
        return new Object[]{
                "sonarqube:8.7-community"
        };
    }

    @Test
    public void testSonarQube() throws Exception {
        String redisUrl = sonarQube.getContainerIpAddress() + ":" + sonarQube.getMappedPort(9000);
        GenericContainer scanner = createScanner("analyze-default");
        scanner.start();

        scanner = createScanner("analyze-branch");
        scanner.start();

        // wait for all ce tasks
        while (tasks.size() != 2) {
            Thread.sleep(1000);
        }

        OkHttpClient client = new OkHttpClient();

        byte[] encodedAuth = Base64.encodeBase64(
                "admin:admin".getBytes(StandardCharsets.ISO_8859_1));
        String authHeader = "Basic " + new String(encodedAuth);
        Request request = new Request.Builder()
                .url("http://" + redisUrl + "/api/project_branches/list?project=branch-scanner")
                .addHeader(HttpHeaders.AUTHORIZATION, authHeader)
                .build();

        try (Response response = client.newCall(request).execute()) {
            BranchesResponse branchesResponse = GsonHelper.create().fromJson(response.body().charStream(), BranchesResponse.class);
            List<Branch> branches = branchesResponse.branches;

            assertEquals(2, branches.size());
            assertThat(branches, CoreMatchers.hasItem(new Branch("master", "BRANCH", true)));
            assertThat(branches, CoreMatchers.hasItem(new Branch("branch-19.100", "BRANCH", false)));
        }
    }

    private static class BranchesResponse {
        private List<Branch> branches = new ArrayList<>();
    }

    private static class Branch {
        private String name;
        private String type;
        private boolean isMain;

        public Branch(String name, String type, boolean isMain) {
            this.name = name;
            this.type = type;
            this.isMain = isMain;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Branch branch = (Branch) o;
            return isMain == branch.isMain &&
                    name.equals(branch.name) &&
                    type.equals(branch.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type, isMain);
        }
    }
}
