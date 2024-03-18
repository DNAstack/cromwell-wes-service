package com.dnastack.wes.service;

import com.dnastack.wes.service.wdl.WdlSupplier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.MultiPartSpecification;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.with;
import static org.hamcrest.Matchers.*;


@DisplayName("WES tests")
public class WesE2ETest extends BaseE2eTest {

    static final List<String> TERMINAL_STATES = List.of("COMPLETE", "EXECUTOR_ERROR", "SYSTEM_ERROR", "CANCELED");
    ConditionFactory pollInterval = with()
        .ignoreException(AssertionError.class)
        .pollDelay(Duration.ofSeconds(2)).and()
        .pollInterval(Duration.ofSeconds(3))
        .atMost(Duration.ofMinutes(5));
    private static final WdlSupplier supplier = new WdlSupplier();

    @Test
    @DisplayName("Can retrieve the service information when unauthorized and the summary is empty")
    public void canGetServiceInfoNoAuth() {
        Assumptions.assumeFalse(authType.equals(AuthType.NO_AUTH));

        //@formatter:off
        given()
            .log().uri()
            .log().method()
            .accept(ContentType.JSON)
        .get("/ga4gh/wes/v1/service-info")
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
        getJsonRequest()
            .get("/ga4gh/wes/v1/service-info")
            .then()
            .assertThat()
            .statusCode(200)
            .body("workflow_type_versions", hasKey("WDL"))
            .body("workflow_type_versions.WDL", allOf(hasItem("draft-2"), hasItem("1.0")))
            .body("supported_wes_versions", hasItem("1.0.0"))
            .body("supported_filesystem_protocols", anyOf(hasItem("file"), hasItem("gs"), hasItem("http"), hasItem("drs")))
            .body("$", hasKey("system_state_counts"));

    }

    private String getResource(String path) {
        return resourceUrl + path;
    }

    @Test
    @DisplayName("Listing all runs unauthorized returns 401 response")
    public void listingRunsUnauthorizedError() {
        Assumptions.assumeFalse(authType.equals(AuthType.NO_AUTH));
        //@formatter:off
        getJsonRequest()
        .get("/ga4gh/wes/v1/runs")
        .then()
            .assertThat()
            .statusCode(401);
        //@formatter:on

    }

    @Test
    @DisplayName("Listing all runs with incorrect resource in access token returns 403 response")
    public void listingRunsIncorrectResourceError() {
        Assumptions.assumeFalse(authType.equals(AuthType.NO_AUTH));

        given()
            .log().uri()
            .log().method()
            .accept(ContentType.JSON)
            .header(getHeader(getResource("/ga4gh/wes/v1/service-info")))
            .get("/ga4gh/wes/v1/runs")
            .then()
            .assertThat()
            .statusCode(403);
    }

    @Test
    @DisplayName("Listing all runs returns response with")
    public void listingRunsReturnsEmptyResponse() {
        //@formatter:off
        getJsonRequest()
        .queryParam("page_size",5)
        .get("/ga4gh/wes/v1/runs")
        .then()
            .assertThat()
            .statusCode(200)
            .body("runs.size()",lessThanOrEqualTo(5));
        //@formatter:on

    }

    private void pollUntilJobCompletes(String workflowJobId) throws Exception {
        waitForRunTerminalState(workflowJobId, "COMPLETE");
    }

    void waitForRunTerminalState(String workflowJobId, String expectedState) {
        String actualState = pollInterval.until(() ->
                getJsonRequest()
                    .get("/ga4gh/wes/v1/runs/{runId}/status", workflowJobId)
                    .then()
                    .statusCode(200)
                    .body("state", in(TERMINAL_STATES))
                    .extract()
                    .jsonPath().getString("state")
            , TERMINAL_STATES::contains);

        Assertions.assertEquals(expectedState, actualState,
            "Expecting run %s to have a terminal state of '%s', however the state is '%s'".formatted(workflowJobId, expectedState, actualState));


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

    @DisplayName("Test Workflow Run Submissions")
    @Nested
    public class WorkflowRunSubmissions {

        @Test
        @DisplayName("Workflow Run Submission with valid payload should succeed")
        public void submitValidWorkflowRun() {
            Map<String, String> tags = Collections.singletonMap("WES", "TestRun");
            Map<String, String> engineParams = new HashMap<>();
            Map<String, String> inputs = Collections.singletonMap("hello_world.name", "Some sort of String");

            //@formatter:off
            getRequest()
                .multiPart(getWorkflowUrlMultipart("echo.wdl"))
                .multiPart(getMultipartAttachment("echo.wdl",supplier.getFileContent(WdlSupplier.WORKFLOW_WITHOUT_FILE).getBytes()))
                .multiPart(getJsonMultipart("workflow_engine_parameters", engineParams))
                .multiPart(getJsonMultipart("tags", tags))
                .multiPart(getJsonMultipart("workflow_params", inputs))
            .post("/ga4gh/wes/v1/runs")
            .then()
                .assertThat()
                .statusCode(200)
                .body("run_id",is(notNullValue()));
            //@formatter:on
        }


        @Test
        @DisplayName("Workflow Run Submission with invalid url and unsupported attachment should fail")
        public void submitWorkflowRunWithInvalidPayloadShouldFail() {
            //@formatter:off
            getRequest()
              .multiPart("workflow_attachment","echo.wdl", supplier.getFileContent(WdlSupplier.WORKFLOW_WITHOUT_FILE).getBytes())
            .post("/ga4gh/wes/v1/runs")
            .then()
              .assertThat()
              .statusCode(400);
            //@formatter:on
        }

        @Test
        @DisplayName("Workflow Run Submission with valid multiple attachments should succeed")
        public void submitValidWorkflowRunWithMultipleAttachments() throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<Map<String, Object>> typeReference = new TypeReference<Map<String, Object>>() {
            };
            Map<String, Object> inputs = mapper
                .readValue(supplier.getFileContent(WdlSupplier.WORKFLOW_WITH_IMPORTS_INPUTS), typeReference);
            //@formatter:off
            getRequest()
                .multiPart(getWorkflowUrlMultipart("echo.wdl"))
                .multiPart(getMultipartAttachment("echo.wdl",supplier.getFileContent(WdlSupplier.WORKFLOW_WITH_IMPORTS_1).getBytes()))
                .multiPart(getMultipartAttachment(WdlSupplier.WORKFLOW_WITH_IMPORTS_2,supplier.getFileContent(WdlSupplier.WORKFLOW_WITH_IMPORTS_2).getBytes()))
                .multiPart(getJsonMultipart("workflow_params", inputs))
            .post("/ga4gh/wes/v1/runs")
            .then()
                .assertThat()
                .statusCode(200)
                .body("run_id",is(notNullValue()));
            //@formatter:on
        }

        @Test
        @DisplayName("Workflow Run Submission with valid multiple attachments should succeed")
        public void submitValidWorkflowRunWithSubWorkflowFlattensTasks() throws Exception {
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<Map<String, Object>> typeReference = new TypeReference<Map<String, Object>>() {
            };
            Map<String, Object> inputs = mapper
                .readValue(supplier.getFileContent(WdlSupplier.WORKFLOW_WITH_IMPORTS_INPUTS), typeReference);
            //@formatter:off
            String workflowJobId = getRequest()
                .multiPart(getWorkflowUrlMultipart("echo.wdl"))
                .multiPart(getMultipartAttachment("echo.wdl",supplier.getFileContent(WdlSupplier.WORKFLOW_WITH_IMPORTS_1).getBytes()))
                .multiPart(getMultipartAttachment(WdlSupplier.WORKFLOW_WITH_IMPORTS_2,supplier.getFileContent(WdlSupplier.WORKFLOW_WITH_IMPORTS_2).getBytes()))
                .multiPart(getJsonMultipart("workflow_params", inputs))
                .post("/ga4gh/wes/v1/runs")
                .then()
                .assertThat()
                .statusCode(200)
                .body("run_id",is(notNullValue()))
                    .extract()

                .jsonPath()
                .getString("run_id");
            //@formatter:on
            pollUntilJobCompletes(workflowJobId);

            final String runLogPath = "/ga4gh/wes/v1/runs/" + workflowJobId;
            getRequest()
                .get(runLogPath)
                .then()
                .assertThat()
                .body("task_logs", hasSize(equalTo(4)));
            //@formatter:on
        }

        @Test
        @DisplayName("Workflow Run Submission with valid payload and options should succeed")
        public void submitValidWorkflowRunWithOptionsAttachment() throws InterruptedException {
            Map<String, String> tags = Collections.singletonMap("WES", "TestRun");
            Map<String, Boolean> engineParams = Collections.singletonMap("write_to_cache", false);
            Map<String, String> inputs = Collections.singletonMap("hello_world.name", "Some sort of String");

            //@formatter:off
            String runId = getRequest()
              .multiPart(getWorkflowUrlMultipart("echo.wdl"))
              .multiPart(getMultipartAttachment("echo.wdl",supplier.getFileContent(WdlSupplier.WORKFLOW_WITHOUT_FILE).getBytes()))
              .multiPart(getMultipartAttachment( "options.json",engineParams))
              .multiPart(getJsonMultipart("tags", tags))
              .multiPart(getJsonMultipart("workflow_params", inputs))
            .post("/ga4gh/wes/v1/runs")
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
            Map<String, String> tags = Collections.singletonMap("WES", "TestRun");
            Map<String, Boolean> engineParams = Collections.singletonMap("write_to_cache", false);
            Map<String, String> inputs = Collections.singletonMap("test.input_file", "name.txt");

            //@formatter:off
            String runId = getRequest()
                .multiPart(getWorkflowUrlMultipart("echo.wdl"))
                .multiPart(getMultipartAttachment("echo.wdl",supplier.getFileContent(WdlSupplier.CAT_FILE_WORKFLOW).getBytes()))
                .multiPart(getMultipartAttachment("name.txt","Frank".getBytes()))
                .multiPart(getJsonMultipart("workflow_engine_parameters",engineParams))
                .multiPart(getJsonMultipart("tags",tags))
                .multiPart(getJsonMultipart("workflow_params",inputs))
            .post("/ga4gh/wes/v1/runs")
            .then()
                .log().everything()
                .assertThat()
                .statusCode(200)
                .body("run_id",is(notNullValue()))
                .extract()
                .jsonPath()
                .getString("run_id");
            //@formatter:on
            pollUntilJobCompletes(runId);

            //@formatter:off
            getJsonRequest()
            .get("/ga4gh/wes/v1/runs/{runId}", runId)
            .then()
            .assertThat()
                .statusCode(200)
                .body("run_id",equalTo(runId))
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
                Map<String, String> tags = Collections.singletonMap("WES", "TestRun");
                Map<String, Boolean> engineParams = Collections.singletonMap("write_to_cache", false);
                Map<String, String> inputs = Collections.singletonMap("hello_world.name", "Some sort of String");

                //@formatter:off
                workflowJobId = getRequest()
                  .multiPart(getWorkflowUrlMultipart("echo.wdl"))
                  .multiPart(getMultipartAttachment("echo.wdl",supplier.getFileContent(WdlSupplier.WORKFLOW_WITHOUT_FILE).getBytes()))
                  .multiPart(getJsonMultipart("workflow_engine_parameters", engineParams))
                  .multiPart(getJsonMultipart("tags", tags))
                  .multiPart(getJsonMultipart("workflow_params", inputs))
                .post("/ga4gh/wes/v1/runs")
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

                //@formatter:off
                getJsonRequest()
                .get("/ga4gh/wes/v1/runs/{runId}", workflowJobId)
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

                //@formatter:off
                getJsonRequest()
                .get("/ga4gh/wes/v1/runs/{runId}/status", workflowJobId )
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

                //@formatter:off
                getJsonRequest()
                .get("/ga4gh/wes/v1/runs/{runId}/status", UUID.randomUUID() )
                .then()
                    .assertThat()
                    .statusCode(anyOf(equalTo(404),equalTo(401)));
                //@formatter:on
            }


            @Test
            @DisplayName("Get Run Log for non-existent run fails with status 401 or 404")
            public void getRunLogForNonExistentRunShouldFail() {

                //@formatter:off
                getJsonRequest()
                .get("/ga4gh/wes/v1/runs/" + UUID.randomUUID())
                .then()
                    .assertThat()
                    .statusCode(anyOf(equalTo(404),equalTo(401)));
                //@formatter:on
            }


            @Test
            @DisplayName("List Runs includes current job")
            public void listRunsReturnsReturnsNonEmptyCollection() {

                //@formatter:off
                getJsonRequest()
                .get("/ga4gh/wes/v1/runs")
                .then()
                    .assertThat()
                    .statusCode(200)
                    .body("runs.size()",greaterThan(0))
                    .body("runs.findAll { it.run_id == /" + workflowJobId +"/ }",notNullValue());
                //@formatter:on
            }


            @ParameterizedTest
            @MethodSource("completeWorkflowWithFilesProvider")
            @DisplayName("Get Run Files for existing run returns all files")
            public void getRunFilesReturnsNonEmptyCollection(String runId) {

                //@formatter:off
                getJsonRequest()
                .get("/ga4gh/wes/v1/runs/{runId}/files", runId)
                .then()
                    .assertThat()
                    .statusCode(200)
                    .body("runFiles.size()", greaterThan(0))
                    .body("runFiles.every { it.path != null && it.file_type in ['FINAL', 'SECONDARY', 'LOG'] }", equalTo(true));
                //@formatter:on
            }

            @ParameterizedTest
            @MethodSource("completeWorkflowWithFilesProvider")
            @DisplayName("Get Run File Metadata for existing run returns metadata")
            public void getFileMetadataForExistingRun(String runId) {

                //@formatter:off
                String filePath = getJsonRequest()
                    .get("/ga4gh/wes/v1/runs/{runId}/files", runId)
                    .then()
                    .assertThat()
                    .statusCode(200)
                    .body("runFiles.size()", greaterThan(0))
                    .body("runFiles.every { it.path != null && it.file_type in ['FINAL', 'SECONDARY', 'LOG'] }", equalTo(true))
                    .extract()
                    .jsonPath().getString("runFiles[0].path");

                getJsonRequest()
                    .queryParam("path",filePath)
                    .get("/ga4gh/wes/v1/runs/{runId}/file",runId)
                    .then()
                    .assertThat()
                    .statusCode(200)
                    .body("path",equalTo(filePath))
                    .body("size",notNullValue())
                    .body("name",notNullValue());

                // Test Json Content
                getJsonRequest()
                    .queryParam("path","$.outputs['hello_world.out']")
                    .get("/ga4gh/wes/v1/runs/{runId}/file",runId)
                    .then()
                    .assertThat()
                    .statusCode(200)
                    .body("path",equalTo(filePath))
                    .body("size",notNullValue())
                    .body("name",notNullValue());

                getJsonRequest()
                    .queryParam("path","/foo/bar/biz/baz")
                    .get("/ga4gh/wes/v1/runs/{runId}/file",runId)
                    .then()
                    .assertThat()
                    .statusCode(404);

                getJsonRequest()
                    .queryParam("path","$.inputs['hello_world.name']")
                    .get("/ga4gh/wes/v1/runs/{runId}/file",runId)
                    .then()
                    .assertThat()
                    .statusCode(404);
                //@formatter:on
            }

            @ParameterizedTest
            @MethodSource("completeWorkflowWithFilesProvider")
            @DisplayName("Stream Run Files for existing run streams contents")
            public void streamFileBytesForExistingRun(String runId) {

                //@formatter:off
                String filePath = getJsonRequest()
                    .get("/ga4gh/wes/v1/runs/{runId}/files",  runId )
                    .then()
                    .assertThat()
                    .statusCode(200)
                    .body("runFiles.size()", greaterThan(0))
                    .body("runFiles.every { it.path != null && it.file_type in ['FINAL', 'SECONDARY', 'LOG'] }", equalTo(true))
                    .extract()
                    .jsonPath().getString("runFiles[0].path");

                getJsonRequest()
                    .accept(ContentType.BINARY)
                    .queryParam("path",filePath)
                    .get("/ga4gh/wes/v1/runs/{runId}/file",runId)
                    .then()
                    .assertThat()
                    .contentType(ContentType.BINARY)
                    .header("Last-Modified",notNullValue())
                    .header("Content-Length",notNullValue())
                    .statusCode(200);

                // Test Json Content
                getJsonRequest()
                    .accept(ContentType.BINARY)
                    .queryParam("path","$.outputs['hello_world.out']")
                    .get("/ga4gh/wes/v1/runs/{runId}/file",runId)
                    .then()
                    .assertThat()
                    .statusCode(200)
                    .contentType(ContentType.BINARY)
                    .header("Last-Modified",notNullValue())
                    .header("Content-Length",notNullValue());
                //@formatter:on
            }


            @Test
            @DisplayName("Get Run Files for non-existent run fails with status 401 or 404")
            public void getRunFilesForNonExistentRunShouldFail() {

                //@formatter:off
                getJsonRequest()
                .get("/ga4gh/wes/v1/runs/{runId}/files", UUID.randomUUID())
                .then()
                    .assertThat()
                    .statusCode(anyOf(equalTo(404), equalTo(401)));
                //@formatter:on
            }


            @ParameterizedTest
            @MethodSource("completeWorkflowWithFilesProvider")
            @DisplayName("Delete Run Files for existing run returns all deleted files")
            public void deleteRunFilesReturnsNonEmptyCollection(String runId) {
                //@formatter:off
                getJsonRequest()
                .delete("/ga4gh/wes/v1/runs/{runId}/files",  runId )
                .then()
                    .assertThat()
                    .statusCode(200)
                    .body("deletions.size()", greaterThan(0))
                    .body("deletions.every { it.path != null && it.file_type == 'SECONDARY' && it.state == 'DELETED' }", equalTo(true));
                //@formatter:on
            }


            @ParameterizedTest
            @MethodSource("completeWorkflowWithFilesProvider")
            @DisplayName("Delete Run Files for existing run asynchronously returns all deleted files")
            public void deleteRunFilesAsyncReturnsNonEmptyCollection(String runId) {
                //@formatter:off
                getJsonRequest()
                .queryParam("async", true)
                .delete("/ga4gh/wes/v1/runs/{runId}/files", runId)
                .then()
                    .assertThat()
                    .statusCode(200)
                    .body("deletions.size()", greaterThan(0))
                    .body("deletions.every { it.path != null && it.file_type == 'SECONDARY' && it.state == 'ASYNC' }", equalTo(true));
                //@formatter:on

                Awaitility.await()
                    .atMost(Duration.ofSeconds(30))
                    .pollInterval(Duration.ofSeconds(5))
                    .untilAsserted(() ->
                        //@formatter:off
                        getJsonRequest()
                        .get("/ga4gh/wes/v1/runs/{runId}/files",  runId )
                        .then()
                            .assertThat()
                            .statusCode(200)
                            .body("runFiles.size()", greaterThan(0))
                            .body("runFiles.every { it.path != null && it.file_type in ['FINAL', 'LOG'] }", equalTo(true)));
                        //@formatter:on
            }


            @Test
            @DisplayName("Delete Run Files for non-existent run fails with status 401 or 404")
            public void deleteRunFilesForNonExistentRunShouldFail() {

                //@formatter:off
                getJsonRequest()
                .delete("/ga4gh/wes/v1/runs/{runId}/files",UUID.randomUUID())
                .then()
                    .assertThat()
                    .statusCode(anyOf(equalTo(404),equalTo(401)));
                //@formatter:on
            }


             Stream<Arguments> completeWorkflowWithFilesProvider() throws Exception {
                Map<String, String> inputs = Collections.singletonMap("hello_world.name", "Some sort of String");

                //@formatter:off
                String workflowJobIdWithAllOutputTypes = getRequest()
                    .multiPart(getWorkflowUrlMultipart("echo.wdl"))
                    .multiPart(getMultipartAttachment("echo.wdl", supplier.getFileContent(WdlSupplier.WORKFLOW_WITH_ALL_OUTPUT_TYPES).getBytes()))
                    .multiPart(getJsonMultipart("workflow_params", inputs))
                .post("/ga4gh/wes/v1/runs")
                .then()
                    .assertThat()
                    .statusCode(200)
                    .body("run_id", is(notNullValue()))
                    .extract()
                    .jsonPath()
                    .getString("run_id");
                //@formatter:on

                pollUntilJobCompletes(workflowJobIdWithAllOutputTypes);
                return Stream.of(Arguments.of(workflowJobIdWithAllOutputTypes));
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
            Map<String, String> tags = Collections.singletonMap("WES", "TestRun");
            Map<String, Boolean> engineParams = Collections.singletonMap("write_to_cache", false);
            Map<String, String> inputs = Collections.singletonMap("hello_world.name", "Frank");

            //@formatter:off
            workflowJobId = getRequest()
                .multiPart(getWorkflowUrlMultipart("echo.wdl"))
                .multiPart(getMultipartAttachment("echo.wdl",supplier.getFileContent(WdlSupplier.WORKFLOW_WITHOUT_FILE).getBytes()))
                .multiPart(getJsonMultipart("workflow_engine_parameters", engineParams))
                .multiPart(getJsonMultipart("tags", tags))
                .multiPart(getJsonMultipart("workflow_params", inputs))
            .post("/ga4gh/wes/v1/runs")
            .then()
                .assertThat()
                .statusCode(200)
                .body("run_id",is(notNullValue()))
                .extract()
                .jsonPath()
                .getString("run_id");
            //@formatter:on
            pollUntilJobCompletes(workflowJobId);
        }

        @Test
        @DisplayName("Get Stdout and Stderr for task")
        public void getStdoutForTaskReturnsSuccessfully() {
            //@formatter:off
            Map<String,String> taskLogs = getJsonRequest()
            .get("/ga4gh/wes/v1/runs/{runId}", workflowJobId)
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
                .assertTrue(taskLogs.get("stderr").endsWith("/ga4gh/wes/v1/runs/" + workflowJobId + "/logs/task/" + taskLogs.get("id") + "/stderr"));
            Assertions
                .assertTrue(taskLogs.get("stdout").endsWith("/ga4gh/wes/v1/runs/" + workflowJobId + "/logs/task/" + taskLogs.get("id") + "/stdout"));

            //@formatter:off
            String body = getRequest()
            .get(taskLogs.get("stdout"))
            .then()
                .statusCode(200)
                .extract().asString();

            Assertions.assertEquals("Hello Frank\n",body);

            // test range offset
            body = getRequest()
                .header("Range","bytes=0-4")
                .get(taskLogs.get("stdout"))
                .then()
                .statusCode(206)
                .extract().asString();

            Assertions.assertEquals("Hello",body);

            body = getRequest()
                .header("Range","bytes=6-")
                .get(taskLogs.get("stdout"))
                .then()
                .statusCode(206)
                .extract().asString();

            Assertions.assertEquals("Frank\n",body);

            body = getRequest()
                .header("Range","bytes=1-3")
                .get(taskLogs.get("stdout"))
                .then()
                .statusCode(206)
                .extract().asString();

            Assertions.assertEquals("ell",body);

            //@formatter:on

            //@formatter:off
            body = getRequest()
            .get(taskLogs.get("stderr"))
            .then()
                .statusCode(200)
                .extract().asString();

            Assertions.assertEquals("Goodbye Frank\n",body);
            //@formatter:on

            // test range offset
            body = getRequest()
                .header("Range", "bytes=0-4")
                .get(taskLogs.get("stderr"))
                .then()
                .statusCode(206)
                .extract().asString();

            Assertions.assertEquals("Goodb", body);

            body = getRequest()
                .header("Range", "bytes=8-")
                .get(taskLogs.get("stderr"))
                .then()
                .statusCode(206)
                .extract().asString();

            Assertions.assertEquals("Frank\n", body);

            body = getRequest()
                .header("Range", "bytes=1-3")
                .get(taskLogs.get("stderr"))
                .then()
                .statusCode(206)
                .extract().asString();

            Assertions.assertEquals("ood", body);

            getRequest()
                .header("Range", "bytes=1-3,6-9")
                .get(taskLogs.get("stderr"))
                .then()
                .statusCode(416);
        }


        @Test
        @DisplayName("Get Stdout and Stderr for task")
        public void unauthenticatedUserCannotAccessLogs() {
            Assumptions.assumeFalse(authType.equals(AuthType.NO_AUTH));
            //@formatter:off
            Map<String,String> taskLogs = getJsonRequest()
            .get("/ga4gh/wes/v1/runs/{runId}", workflowJobId)
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

}
