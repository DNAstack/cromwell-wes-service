package com.dnastack.wes.service;

import com.dnastack.wes.service.utils.AuthorizationClient;
import com.dnastack.wes.service.utils.WdlSupplier;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.nimbusds.jose.util.IOUtils;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.hamcrest.Matchers.*;


@DisplayName("WES tests")
public class WesE2ETest extends BaseE2eTest {

    private static final String DEFAULT_PRIVATE_KEY_FILE = "jwt.pem";
    private static AuthorizationClient authorizationClient;

    private String getRootPath() {
        return "/ga4gh/wes/v1";
    }

    @BeforeAll
    public static void setupTests() {
        try {
            authorizationClient = new AuthorizationClient();
//            authorizationClient = new AuthorizationClient(loadPrivateKey());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private String getAccessToken() throws IOException {
        final GoogleCredentials credentials = getCredentials();
        if (credentials.getAccessToken() != null) {
            return credentials.getAccessToken().getTokenValue();
        } else {
            final AccessToken accessToken = credentials.refreshAccessToken();
            org.junit.jupiter.api.Assertions.assertNotNull(accessToken, "Unable to obtain access token for test");

            return accessToken.getTokenValue();
        }
    }

    private GoogleCredentials getCredentials() throws IOException {
        final String envCredentials = optionalEnv("E2E_GOOGLE_APPLICATION_CREDENTIALS", null);
        final GoogleCredentials credentials;
        if (envCredentials != null) {
            credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(envCredentials.getBytes()));
        } else {
            credentials = GoogleCredentials.getApplicationDefault();
        }

        if (credentials.createScopedRequired()) {
            return credentials.createScoped("https://www.googleapis.com/auth/cloud-platform");
        } else {
            return credentials;
        }
    }

    private static String loadPrivateKey() throws IOException {
        String privKey = optionalEnv("E2E_WES_PRIVATE_KEY", null);

        if (privKey == null) {
            InputStream inputStream = WesE2ETest.class.getResourceAsStream(DEFAULT_PRIVATE_KEY_FILE);
            privKey = IOUtils.readInputStreamToString(inputStream, Charset.forName("UTF-8"));
        }
        return privKey;
    }

    @RequiredArgsConstructor
    @Getter
    private static class EarlyAbortException extends Exception {
        private final AssertionError cause;
    }

    @FunctionalInterface
    interface Assertions {
        void run() throws EarlyAbortException;
    }

    private static void poll(Duration duration, Assertions assertions) throws Exception {
        final Instant start = Instant.now();
        try {
            while (true) {
                try {
                    Thread.sleep(500);
                    assertions.run();
                    return;
                } catch (AssertionError ae) {
                    if (Instant.now().isAfter(start.plus(duration))) {
                        throw ae;
                    }
                } catch (EarlyAbortException e) {
                    throw e.getCause();
                }
            }
        } finally {
            System.out.printf("Polling finished after %d seconds\n", Duration.between(start, Instant.now()).getSeconds());
        }
    }

    @Test
    @DisplayName("Can retrieve the service information when unauthorized and the summary is empty")
    public void canGetServiceInfoNoAuth() {
        String path = getRootPath() + "/service-info";

        //@formatter:off
        given()
            .log().uri()
            .log().method()
            .accept(ContentType.JSON)
        .get(path)
        .then()
            .assertThat()
            .statusCode(200)
            .body("workflow_type_versions",hasKey("WDL"))
            .body("workflow_type_versions.WDL",allOf(hasItem("draft-2"),hasItem("1.0")))
            .body("supported_wes_versions",hasItem("1.0.0"))
            .body("supported_filesystem_protocols",anyOf(hasItem("file"),hasItem("gs"),hasItem("http"),hasItem("drs")))
            .body("$", not(hasKey("system_state_counts")))
            .body("auth_instruction_url",not(isEmptyOrNullString()));
         //@formatter:on

    }

    @Test
    @DisplayName("Can retrieve the service information when authorized and the summary is present")
    public void canGetServiceInfoWithAuthAndSummaryPresent() {
        String path = getRootPath() + "/service-info";

        //@formatter:off
        given()
            .log().uri()
            .log().method()
            .accept(ContentType.JSON)
            .header(authorizationClient.getHeader())
        .get(path)
        .then()
            .assertThat()
            .statusCode(200)
            .body("workflow_type_versions",hasKey("WDL"))
            .body("workflow_type_versions.WDL",allOf(hasItem("draft-2"),hasItem("1.0")))
            .body("supported_wes_versions",hasItem("1.0.0"))
            .body("supported_filesystem_protocols",anyOf(hasItem("file"),hasItem("gs"),hasItem("http"),hasItem("drs")))
            .body("$", hasKey("system_state_counts"));
        //@formatter:on

    }

    @Test
    @DisplayName("Listing all runs unauthorized returns 401 response")
    public void listingRunsUnauthorizedError() {
        String path = getRootPath() + "/runs";
        //@formatter:off
        given()
            .redirects()
            .follow(false)
            .log().uri()
            .log().method()
            .accept(ContentType.JSON)
        .get(path)
        .then()
            .assertThat()
            .statusCode(401);
        //@formatter:on

    }


    @Test
    @DisplayName("Listing all runs returns response with")
    public void listingRunsReturnsEmptyResponse() {
        String path = getRootPath() + "/runs";
        //@formatter:off
        given()
            .log().uri()
            .log().method()
            .header(authorizationClient.getHeader())
            .queryParam("page_size",5)
            .accept(ContentType.JSON)
        .get(path)
        .then()
            .assertThat()
            .statusCode(200)
            .body("runs.size()",lessThanOrEqualTo(5));
        //@formatter:on

    }


    @DisplayName("Test Workflow Run Submissions")
    @Nested
    public class WorkflowRunSubmissions {

        @Test
        @DisplayName("Workflow Run Submission with valid payload should succeed")
        public void submitValidWorkflowRun() {
            String path = getRootPath() + "/runs";
            Map<String, String> tags = Collections.singletonMap("WES", "TestRun");
            Map<String, String> engineParams = new HashMap<>();
            Map<String, String> inputs = Collections.singletonMap("hello_world.name", "Some sort of String");

            //@formatter:off
            given()
                .log().uri()
                .log().method()
                .header(authorizationClient.getHeader())
                .multiPart("workflow_url","echo.wdl")
                .multiPart("workflow_attachment","echo.wdl",WdlSupplier.WORKFLOW_WITHOUT_FILE.getBytes())
                .multiPart("workflow_engine_parameters", engineParams,ContentType.JSON.toString())
                .multiPart("tags", tags,ContentType.JSON.toString())
                .multiPart("workflow_params", inputs,ContentType.JSON.toString())
            .post(path)
            .then()
                .assertThat()
                .statusCode(200)
                .body("run_id",is(notNullValue()));
            //@formatter:on
        }

        @Test
        @DisplayName("Workflow Run Submission with GCP access token input")
        public void submitWorkflowRunNeedingObjectTransfer() throws Exception {
            final Boolean testObjectTransfer = Boolean.valueOf(optionalEnv("E2E_TEST_OBJECT_TRANSFER", "true"));
            Assumptions.assumeTrue(testObjectTransfer, "GCP object transfer test has been disabled");
            final String submitPath = getRootPath() + "/runs";
            final Map<String, String> tags = Collections.singletonMap("WES", "TestRun");
            final Map<String, String> engineParams = new HashMap<>();
            final String inputFile = optionalEnv("E2E_WORKFLOW_INPUT", "gs://ddap-e2etest-objects/small_files/SAMPLE_TEST_aligned.sorted.bam");
            final Map<String, String> inputs = Collections.singletonMap("md5Sum.inputFile", inputFile);
            final String token = getAccessToken();
            final String tokens = format("{\"%s\": \"%s\"}", inputFile, token);

            //@formatter:off
            final ExtractableResponse<Response> runSubmitResponse =
                given()
                    .log().uri()
                    .log().method()
                    .header(authorizationClient.getHeader())
                    .multiPart("workflow_url", "workflow.wdl")
                    .multiPart("workflow_attachment", "workflow.wdl", WdlSupplier.MD5_SUM_WDL.getBytes())
                    .multiPart("workflow_engine_parameters", engineParams, ContentType.JSON.toString())
                    .multiPart("tags", tags, ContentType.JSON.toString())
                    .multiPart("workflow_params", inputs, ContentType.JSON.toString())
                    .multiPart("workflow_attachment", "tokens.json", tokens.getBytes())
                    .post(submitPath)
                    .then()
                    .log().ifValidationFails(LogDetail.ALL)
                    .assertThat()
                    .statusCode(200)
                    .body("run_id", is(notNullValue()))
                    .extract();
            //@formatter:on

            final String runId = runSubmitResponse.body()
                                         .jsonPath()
                                         .getString("run_id");
            final String runPathStatus = format("%s/%s/status", submitPath, runId);

            poll(Duration.ofMinutes(6), () -> {
                //@formatter:off
                final ExtractableResponse<Response> statusResponse =
                given()
                        .log().uri()
                        .log().method()
                        .header(authorizationClient.getHeader())
                        .get(runPathStatus)
                        .then()
                        .assertThat()
                        .statusCode(200)
                        .body("run_id", equalTo(runId))
                        .extract();
                //@formatter:on
                final String state = statusResponse.body()
                                                   .jsonPath()
                                                   .getString("state");
                System.out.println("Workflow Run State: " + state);

                if ("EXECUTION_ERROR".equals(state) || "CANCELED".equals(state)) {
                    throw new EarlyAbortException(new AssertionError("Run failed with status " + state));
                } else {
                    org.junit.jupiter.api.Assertions.assertEquals("COMPLETE", state, format("Run [%s] not in expected state", runId));
                }
            });
        }

        @Test
        @DisplayName("Workflow Run Submission with invalid url and unsupported attachment should fail")
        public void submitWorkflowRunWithInvalidPayloadShouldFail() {
            String path = getRootPath() + "/runs";
            //@formatter:off
            given()
              .log().uri()
              .log().method()
              .header(authorizationClient.getHeader())
              .multiPart("workflow_attachment","echo.wdl", WdlSupplier.WORKFLOW_WITHOUT_FILE.getBytes())
            .post(path)
            .then()
              .assertThat()
              .statusCode(400);
            //@formatter:on
        }

        @Test
        @DisplayName("Workflow Run Submission with valid multiple attachments should succeed")
        public void submitValidWorkflowRunWithMultipleAttachments() {
            String path = getRootPath() + "/runs";

            //@formatter:off
            given()
                .log().uri()
                .log().method()
                .header(authorizationClient.getHeader())
                .multiPart("workflow_url","echo.wdl")
                .multiPart("workflow_attachment","echo.wdl",WdlSupplier.ECHO_WITH_IMPORT_WDL.getBytes())
                .multiPart("workflow_attachment","struct_test.wdl",WdlSupplier.STRUCT_TEST_WDL.getBytes())
                .multiPart("workflow_params", WdlSupplier.ECHO_WITH_IMPORT_INPUTS,ContentType.JSON.toString())
            .post(path)
            .then()
                .assertThat()
                .statusCode(200)
                .body("run_id",is(notNullValue()));
            //@formatter:on
        }

        @Test
        @DisplayName("Workflow Run Submission with valid payload and options should succeed")
        public void submitValidWorkflowRunWithOptionsAttachment() throws InterruptedException {
            String path = getRootPath() + "/runs";
            Map<String, String> tags = Collections.singletonMap("WES", "TestRun");
            Map<String, Boolean> engineParams = Collections.singletonMap("write_to_cache", false);
            Map<String, String> inputs = Collections.singletonMap("hello_world.name", "Some sort of String");

            //@formatter:off
            String runId = given()
              .log().uri()
              .log().method()
              .header(authorizationClient.getHeader())
              .multiPart("workflow_url","echo.wdl")
              .multiPart("workflow_attachment","echo.wdl",WdlSupplier.WORKFLOW_WITHOUT_FILE.getBytes())
              .multiPart("workflow_attachment", "options.json",engineParams,ContentType.JSON.toString())
              .multiPart("tags", tags,ContentType.JSON.toString())
              .multiPart("workflow_params", inputs,ContentType.JSON.toString())
            .post(path)
            .then()
                .assertThat()
                .statusCode(200)
                .body("run_id",is(notNullValue()))
                .extract()
                .jsonPath()
                .getString("run_id");
            //@formatter:on

        }


        @Nested
        @DisplayName("Test run methods where Workflows have previously been submitted")
        @TestInstance(Lifecycle.PER_CLASS)
        public class RunMethodsWithExistingJobs {

            String workflowJobId;

            @BeforeAll
            public void setup() throws InterruptedException {
                String path = getRootPath() + "/runs";
                Map<String, String> tags = Collections.singletonMap("WES", "TestRun");
                Map<String, Boolean> engineParams = Collections.singletonMap("write_to_cache", false);
                Map<String, String> inputs = Collections.singletonMap("hello_world.name", "Some sort of String");

                //@formatter:off
                workflowJobId = given()
                  .log().uri()
                  .log().method()
                  .header(authorizationClient.getHeader())
                  .multiPart("workflow_url","echo.wdl")
                  .multiPart("workflow_attachment","echo.wdl",WdlSupplier.WORKFLOW_WITHOUT_FILE.getBytes())
                  .multiPart("workflow_engine_parameters", engineParams,ContentType.JSON.toString())
                  .multiPart("tags", tags,ContentType.JSON.toString())
                  .multiPart("workflow_params", inputs,ContentType.JSON.toString())
                .post(path)
                .then()
                    .assertThat()
                    .statusCode(200)
                    .body("run_id",is(notNullValue()))
                    .extract()
                    .jsonPath()
                    .getString("run_id");
                //@formatter:on

                Thread.sleep(15000L);
            }


            @Test
            @DisplayName("Get Run Log Shows extended information appropriately")
            public void getRunLogReturnsAccurateData() {
                String path = getRootPath() + "/runs/" + workflowJobId;

                //@formatter:off
                given()
                    .log().uri()
                    .log().method()
                    .header(authorizationClient.getHeader())
                    .accept(ContentType.JSON)
                .get(path)
                .then()
                    .assertThat()
                    .statusCode(200)
                    .body("run_id",equalTo(workflowJobId))
                    .body("request.workflow_engine_parameters.write_to_cache",equalTo("false"))
                    .body("request.tags.WES",equalTo("TestRun"))
                    .body("state",notNullValue())
                    .body("run_log",not(isEmptyOrNullString()));
                //@formatter:on

            }

            @Test
            @DisplayName("Get Run Status for existing run returns current state")
            public void getRunStatusReturnsJobStatus() {
                String path = getRootPath() + "/runs/" + workflowJobId + "/status";

                //@formatter:off
                given()
                    .log().uri()
                    .log().method()
                    .header(authorizationClient.getHeader())
                    .accept(ContentType.JSON)
                .get(path)
                .then()
                    .assertThat()
                    .statusCode(200)
                    .body("run_id",equalTo(workflowJobId))
                    .body("state",notNullValue());
                //@formatter:on
            }


            @Test
            @DisplayName("Get Run Status for non-existent run fails with status 404")
            public void getRunStatusForNonExistentRunShouldFail() {
                String path = getRootPath() + "/runs/" + -1 + "/status";

                //@formatter:off
                given()
                    .log().uri()
                    .log().method()
                    .header(authorizationClient.getHeader())
                    .accept(ContentType.JSON)
                .get(path)
                .then()
                    .assertThat()
                    .statusCode(404);
                //@formatter:on
            }


            @Test
            @DisplayName("Get Run Log for non-existent run fails with status 404")
            public void getRunLogForNonExistentRunShouldFail() {
                String path = getRootPath() + "/runs/" + -1;

                //@formatter:off
                given()
                    .log().uri()
                    .log().method()
                    .header(authorizationClient.getHeader())
                    .accept(ContentType.JSON)
                .get(path)
                .then()
                    .assertThat()
                    .statusCode(404);
                //@formatter:on
            }


            @Test
            @DisplayName("List Runs includes current job")
            public void listRunsReturnsReturnsNonEmptyCollection() {
                String path = getRootPath() + "/runs";

                //@formatter:off
                given()
                    .log().uri()
                    .log().method()
                    .header(authorizationClient.getHeader())
                    .accept(ContentType.JSON)
                .get(path)
                .then()
                    .assertThat()
                    .statusCode(200)
                    .body("runs.size()",greaterThan(0))
                    .body("runs.findAll { it.run_id == /" + workflowJobId +"/ }",notNullValue());
                //@formatter:on
            }

        }

    }

}
