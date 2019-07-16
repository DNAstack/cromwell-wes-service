package com.dnastack.wes.service;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import com.dnastack.wes.service.utils.Oauth2Client;
import com.dnastack.wes.service.utils.WdlSupplier;
import io.restassured.http.ContentType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@DisplayName("WES tests")
public class WesE2ETest extends BaseE2eTest {


    static Oauth2Client oauth2Client;

    private String getRootPath() {
        return "/ga4gh/wes/v1";
    }

    @BeforeAll
    public static void setupOauth2Client() {
        oauth2Client = new Oauth2Client(requiredEnv("E2E_OAUTH_TOKEN_URL"), requiredEnv("E2E_OAUTH_CLIENTID"), requiredEnv("E2E_OAUTH_USERNAME"), requiredEnv("E2E_OAUTH_PASSWORD"));
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
            .body("supported_filesystem_protocols",allOf(hasItem("gs"),hasItem("http"),hasItem("drs")))
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
            .header(oauth2Client.getHeader())
        .get(path)
        .then()
            .assertThat()
            .statusCode(200)
            .body("workflow_type_versions",hasKey("WDL"))
            .body("workflow_type_versions.WDL",allOf(hasItem("draft-2"),hasItem("1.0")))
            .body("supported_wes_versions",hasItem("1.0.0"))
            .body("supported_filesystem_protocols",allOf(hasItem("gs"),hasItem("http"),hasItem("drs")))
            .body("$", hasKey("system_state_counts"));
        //@formatter:on

    }

    @Test
    @DisplayName("Listing all runs unauthorized returns 401 response")
    public void listingRunsUnauthorizedError() {
        String path = getRootPath() + "/runs";
        //@formatter:off
        given()
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
            .header(oauth2Client.getHeader())
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
                .header(oauth2Client.getHeader())
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
        @DisplayName("Workflow Run Submission with invalid url and unsupported attachment should fail")
        public void submitWorkflowRunWithInvalidPayloadShouldFail() {
            String path = getRootPath() + "/runs";
            //@formatter:off
            given()
              .log().uri()
              .log().method()
              .header(oauth2Client.getHeader())
              .multiPart("workflow_attachment","echo.wdl",WdlSupplier.WORKFLOW_WITHOUT_FILE.getBytes())
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
                .header(oauth2Client.getHeader())
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
              .header(oauth2Client.getHeader())
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
                  .header(oauth2Client.getHeader())
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
                    .header(oauth2Client.getHeader())
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
                    .header(oauth2Client.getHeader())
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
                    .header(oauth2Client.getHeader())
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
                    .header(oauth2Client.getHeader())
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
                    .header(oauth2Client.getHeader())
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
