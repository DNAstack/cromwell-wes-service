package com.dnastack.wes.service;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import com.dnastack.wes.service.utils.AuthorizationClient;
import com.dnastack.wes.service.wdl.WdlSupplier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.nimbusds.jose.util.IOUtils;
import io.restassured.RestAssured;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.MultiPartSpecification;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;


@DisplayName("WES tests")
public class WesE2ETest extends BaseE2eTest {

    private static final String DEFAULT_PRIVATE_KEY_FILE = "jwt.pem";
    private static AuthorizationClient authorizationClient;
    private static WdlSupplier supplier = new WdlSupplier();

    private String getRootPath() {
        return "/ga4gh/wes/v1";
    }

    @BeforeAll
    public static void setupTests() {
        try {
            authorizationClient = new AuthorizationClient();
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
            Assertions.assertNotNull(accessToken, "Unable to obtain access token for test");

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

        return credentials.createScoped("https://www.googleapis.com/auth/cloud-platform", "openid", "email");
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
    interface CustomAssertions {

        void run() throws EarlyAbortException;
    }

    private static void poll(Duration duration, CustomAssertions customAssertions) throws Exception {
        final Instant start = Instant.now();
        try {
            while (true) {
                try {
                    Thread.sleep(500);
                    customAssertions.run();
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
            System.out
                .printf("Polling finished after %d seconds\n", Duration.between(start, Instant.now()).getSeconds());
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
    @DisplayName("Listing all runs with incorrect scope in access token returns 403 response")
    public void listingRunsIncorrectScopeError() {
        String path = getRootPath() + "/runs";
        String resources = RestAssured.baseURI + "/ga4gh/wes/v1/runs/";
        Set<String> scopes = Set.of("write:execution");

        given()
            .log().uri()
            .log().method()
            .accept(ContentType.JSON)
            .header(authorizationClient.getHeader(resources, scopes))
        .get(path)
        .then()
            .assertThat()
            .statusCode(403);
    }


    @Test
    @DisplayName("Listing all runs with incorrect resource in access token returns 403 response")
    public void listingRunsIncorrectResourceError() {
        String path = getRootPath() + "/runs";
        String resources = RestAssured.baseURI + "/ga4gh/wes/v1";
        Set<String> scopes = Set.of("read:execution");

        given()
                .log().uri()
                .log().method()
                .accept(ContentType.JSON)
                .header(authorizationClient.getHeader(resources, scopes))
                .get(path)
                .then()
                .assertThat()
                .statusCode(403);
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
                .multiPart(getWorkflowUrlMultipart("echo.wdl"))
                .multiPart(getMultipartAttachment("echo.wdl",supplier.getFileContent(WdlSupplier.WORKFLOW_WITHOUT_FILE).getBytes()))
                .multiPart(getJsonMultipart("workflow_engine_parameters", engineParams))
                .multiPart(getJsonMultipart("tags", tags))
                .multiPart(getJsonMultipart("workflow_params", inputs))
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

            final Map<String, String> objectAccessCredentials = Collections.singletonMap("accessToken", token);
            JSONObject credentials = new JSONObject();
            credentials.put(inputFile, objectAccessCredentials);

            //@formatter:off
            final ExtractableResponse<Response> runSubmitResponse =
                given()
                    .log().uri()
                    .log().method()
                    .header(authorizationClient.getHeader())
                    .multiPart(getWorkflowUrlMultipart( "workflow.wdl"))
                    .multiPart(getMultipartAttachment( "workflow.wdl", supplier.getFileContent(WdlSupplier.MD5_SUM_WORKFLOW).getBytes()))
                    .multiPart(getMultipartAttachment( "credentials.json", credentials.toJSONString().getBytes()))
                    .multiPart(getJsonMultipart("workflow_engine_parameters", engineParams))
                    .multiPart(getJsonMultipart("tags", tags))
                    .multiPart(getJsonMultipart("workflow_params", inputs))
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
            pollUntilJobCompletes(runId);
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
              .multiPart("workflow_attachment","echo.wdl", supplier.getFileContent(WdlSupplier.WORKFLOW_WITHOUT_FILE).getBytes())
            .post(path)
            .then()
              .assertThat()
              .statusCode(400);
            //@formatter:on
        }

        @Test
        @DisplayName("Workflow Run Submission with valid multiple attachments should succeed")
        public void submitValidWorkflowRunWithMultipleAttachments() throws IOException {
            String path = getRootPath() + "/runs";
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<Map<String, Object>> typeReference = new TypeReference<Map<String, Object>>() {
            };
            Map<String, Object> inputs = mapper
                .readValue(supplier.getFileContent(WdlSupplier.WORKFLOW_WITH_IMPORTS_INPUTS), typeReference);
            //@formatter:off
            given()
                .log().uri()
                .log().method()
                .header(authorizationClient.getHeader())
                .multiPart(getWorkflowUrlMultipart("echo.wdl"))
                .multiPart(getMultipartAttachment("echo.wdl",supplier.getFileContent(WdlSupplier.WORKFLOW_WITH_IMPORTS_1).getBytes()))
                .multiPart(getMultipartAttachment(WdlSupplier.WORKFLOW_WITH_IMPORTS_2,supplier.getFileContent(WdlSupplier.WORKFLOW_WITH_IMPORTS_2).getBytes()))
                .multiPart(getJsonMultipart("workflow_params", inputs))
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
              .multiPart(getWorkflowUrlMultipart("echo.wdl"))
              .multiPart(getMultipartAttachment("echo.wdl",supplier.getFileContent(WdlSupplier.WORKFLOW_WITHOUT_FILE).getBytes()))
              .multiPart(getMultipartAttachment( "options.json",engineParams))
              .multiPart(getJsonMultipart("tags", tags))
              .multiPart(getJsonMultipart("workflow_params", inputs))
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

        @Test
        @DisplayName("Uploading attachment file can be used as workflow input")
        public void uploadWorkflowAttachmentWithRunSubmission() throws Exception {
            String path = getRootPath() + "/runs";
            Map<String, String> tags = Collections.singletonMap("WES", "TestRun");
            Map<String, Boolean> engineParams = Collections.singletonMap("write_to_cache", false);
            Map<String, String> inputs = Collections.singletonMap("test.input_file", "name.txt");

            //@formatter:off
            String workflowJobId = given()
                .log().uri()
                .log().everything()
                .header(authorizationClient.getHeader())
                .multiPart(getWorkflowUrlMultipart("echo.wdl"))
                .multiPart(getMultipartAttachment("echo.wdl",supplier.getFileContent(WdlSupplier.CAT_FILE_WORKFLOW).getBytes()))
                .multiPart(getMultipartAttachment("name.txt","Frank".getBytes()))
                .multiPart(getJsonMultipart("workflow_engine_parameters",engineParams))
                .multiPart(getJsonMultipart("tags",tags))
                .multiPart(getJsonMultipart("workflow_params",inputs))
            .post(path)
            .then()
                .assertThat()
                .statusCode(200)
                .body("run_id",is(notNullValue()))
                .extract()
                .jsonPath()
                .getString("run_id");
            //@formatter:on
            pollUntilJobCompletes(workflowJobId);

            path = getRootPath() + "/runs/" + workflowJobId;
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
                .body("state",equalTo("COMPLETE"))
                .body("run_log",not(isEmptyOrNullString()))
                .body("outputs[\"test.o\"]",equalTo("Frank"));
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
                  .multiPart(getWorkflowUrlMultipart("echo.wdl"))
                  .multiPart(getMultipartAttachment("echo.wdl",supplier.getFileContent(WdlSupplier.WORKFLOW_WITHOUT_FILE).getBytes()))
                  .multiPart(getJsonMultipart("workflow_engine_parameters", engineParams))
                  .multiPart(getJsonMultipart("tags", tags))
                  .multiPart(getJsonMultipart("workflow_params", inputs))
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
            @DisplayName("Get Run Status for non-existent run fails with status 401 or 404")
            public void getRunStatusForNonExistentRunShouldFail() {
                String path = getRootPath() + "/runs/" + UUID.randomUUID() + "/status";

                //@formatter:off
                given()
                    .log().uri()
                    .log().method()
                    .header(authorizationClient.getHeader())
                    .accept(ContentType.JSON)
                .get(path)
                .then()
                    .assertThat()
                    .statusCode(anyOf(equalTo(404),equalTo(401)));
                //@formatter:on
            }


            @Test
            @DisplayName("Get Run Log for non-existent run fails with status 401 or 404")
            public void getRunLogForNonExistentRunShouldFail() {
                String path = getRootPath() + "/runs/" + UUID.randomUUID();

                //@formatter:off
                given()
                    .log().uri()
                    .log().method()
                    .header(authorizationClient.getHeader())
                    .accept(ContentType.JSON)
                .get(path)
                .then()
                    .assertThat()
                    .statusCode(anyOf(equalTo(404),equalTo(401)));
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

    @DisplayName("Test Workflow Log Access")
    @Nested
    @TestInstance(Lifecycle.PER_CLASS)
    public class WorkflowLogAccess {

        String workflowJobId;

        @BeforeAll
        public void setup() throws Exception {
            String path = getRootPath() + "/runs";
            Map<String, String> tags = Collections.singletonMap("WES", "TestRun");
            Map<String, Boolean> engineParams = Collections.singletonMap("write_to_cache", false);
            Map<String, String> inputs = Collections.singletonMap("hello_world.name", "Frank");

            //@formatter:off
            workflowJobId = given()
                .log().uri()
                .log().method()
                .header(authorizationClient.getHeader())
                .multiPart(getWorkflowUrlMultipart("echo.wdl"))
                .multiPart(getMultipartAttachment("echo.wdl",supplier.getFileContent(WdlSupplier.WORKFLOW_WITHOUT_FILE).getBytes()))
                .multiPart(getJsonMultipart("workflow_engine_parameters", engineParams))
                .multiPart(getJsonMultipart("tags", tags))
                .multiPart(getJsonMultipart("workflow_params", inputs))
            .post(path)
            .then()
                .assertThat()
                .statusCode(200)
                .body("run_id",is(notNullValue()))
                .extract()
                .jsonPath()
                .getString("run_id");
            //@formatter:on
            final String runPathStatus = format("%s/%s/status", path, workflowJobId);
            pollUntilJobCompletes(workflowJobId);
        }

        @Test
        @DisplayName("Get Stdout and Stderr for task")
        public void getStdoutForTaskReturnsSuccessfully() {
            String path = getRootPath() + "/runs/" + workflowJobId;
            //@formatter:off
            Map<String,String> taskLogs = given()
                .log().uri()
                .log().method()
                .header(authorizationClient.getHeader())
                .accept(ContentType.JSON)
            .get(path)
            .then()
                .assertThat()
                .statusCode(200)
                .body("run_id",equalTo(workflowJobId))
                .body("state",equalTo("COMPLETE"))
                .extract()
                .jsonPath()
                .getMap("task_logs[0]",String.class,String.class);
            //@formatter:on

            Assertions.assertNotNull(taskLogs.get("stderr"));
            Assertions.assertNotNull(taskLogs.get("stdout"));
            Assertions
                .assertTrue(taskLogs.get("stderr").endsWith(path + "/logs/task/" + taskLogs.get("name") + "/0/stderr"));
            Assertions
                .assertTrue(taskLogs.get("stdout").endsWith(path + "/logs/task/" + taskLogs.get("name") + "/0/stdout"));

            //@formatter:off
            String body = given()
                .log().uri()
                .log().method()
                .header(authorizationClient.getHeader())
            .get(taskLogs.get("stdout"))
            .then()
                .statusCode(200)
                .extract().asString();

            Assertions.assertEquals("Hello Frank\n",body);

            //@formatter:on

            //@formatter:off
            body = given()
                .log().uri()
                .log().method()
                .header(authorizationClient.getHeader())
            .get(taskLogs.get("stderr"))
            .then()
                .statusCode(200)
                .extract().asString();

            Assertions.assertEquals("Goodbye Frank\n",body);
            //@formatter:on
        }

        @Test
        @DisplayName("Get Stdout and Stderr for task")
        public void unauthenticatedUserCannotAccessLogs() {
            String path = getRootPath() + "/runs/" + workflowJobId;
            //@formatter:off
            Map<String,String> taskLogs = given()
                .log().uri()
                .log().method()
                .header(authorizationClient.getHeader())
                .accept(ContentType.JSON)
            .get(path)
            .then()
                .assertThat()
                .statusCode(200)
                .body("run_id",equalTo(workflowJobId))
                .body("state",equalTo("COMPLETE"))
                .extract()
                .jsonPath()
                .getMap("task_logs[0]",String.class,String.class);
            //@formatter:on
            //@formatter:off
            given()
                .log().uri()
                .log().method()
            .get(taskLogs.get("stdout"))
            .then()
                .statusCode(401);

            given()
                .log().uri()
                .log().method()
            .get(taskLogs.get("stderr"))
            .then()
                .statusCode(401);
            //@formatter:on

        }
    }

    private void pollUntilJobCompletes(String workflowJobId) throws Exception {
        String runPathStatus = getRootPath() + "/runs/" + workflowJobId + "/status";
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
                    .body("run_id", equalTo(workflowJobId))
                    .extract();
            //@formatter:on
            final String state = statusResponse.body()
                .jsonPath()
                .getString("state");
            System.out.println("Workflow Run State: " + state);

            if ("EXECUTOR_ERROR".equals(state) || "CANCELED".equals(state) || "CANCELINGSTATE".equals(state)
                || "SYSTEMERROR".equals(state)) {
                throw new EarlyAbortException(new AssertionError("Run failed with status " + state));
            } else {
                Assertions.assertEquals("COMPLETE", state, format("Run [%s] not in expected state", workflowJobId));
            }
        });
    }

    private MultiPartSpecification getWorkflowUrlMultipart(String inputString) {
        return new MultiPartSpecBuilder(inputString)
            .controlName("workflow_url")
            .mimeType(ContentType.TEXT.toString())
            .charset(StandardCharsets.UTF_8)
            .emptyFileName()
            .build();
    }

    private MultiPartSpecification getJsonMultipart(String controlName, Map<String, ?> content) {
        return new MultiPartSpecBuilder(content)
            .controlName(controlName)
            .mimeType(ContentType.JSON.toString())
            .charset(StandardCharsets.UTF_8)
            .emptyFileName()
            .build();
    }

    private MultiPartSpecification getMultipartAttachment(String fileName, Map<String, ?> content) {
        return new MultiPartSpecBuilder(content)
            .controlName("workflow_attachment")
            .fileName(fileName)
            .mimeType(ContentType.JSON.toString())
            .charset(StandardCharsets.UTF_8)
            .emptyFileName()
            .build();
    }

    private MultiPartSpecification getMultipartAttachment(String fileName, byte[] content) {
        return new MultiPartSpecBuilder(content)
            .controlName("workflow_attachment")
            .mimeType(ContentType.BINARY.toString())
            .charset(StandardCharsets.UTF_8)
            .fileName(fileName)
            .build();
    }
}
