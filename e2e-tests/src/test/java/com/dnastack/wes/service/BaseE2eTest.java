package com.dnastack.wes.service;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.EncoderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;

import java.util.Objects;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class BaseE2eTest {

    protected static String tokenUri;
    protected static String clientId;
    protected static String clientSecret;
    protected static String resourceUrl;
    protected static String scopes;
    protected static AuthType authType;

    enum AuthType {
        NO_AUTH,
        OAUTH2,
    }

    @BeforeAll
    public static void setUpAllForAllTests() throws Exception {
        tokenUri = optionalEnv("E2E_TOKEN_URI","http://localhost:8081/oauth/token");
        clientId = optionalEnv("E2E_CLIENT_ID", "wes-service-e2e-test");
        clientSecret = optionalEnv("E2E_CLIENT_SECRET", "dev-secret-never-use-in-prod");
        scopes = optionalEnv("E2E_CLIENT_SCOPES", null);
        resourceUrl = optionalEnv("E2E_CLIENT_RESOURCE_BASE_URI","http://localhost:8090");
        authType = AuthType.valueOf(optionalEnv("E2E_AUTH_TYPE","OAUTH2"));

        RestAssured.baseURI = optionalEnv("E2E_BASE_URI", "http://localhost:8090");
        RestAssured.config = RestAssuredConfig.config()
            .encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false));
    }

    public static String requiredEnv(String name) {
        String val = System.getenv(name);
        if (val == null) {
            fail("Environment variable `" + name + "` is required");
        }
        return val;
    }

    public static String optionalEnv(String name, String defaultValue) {
        String val = System.getenv(name);
        if (val == null) {
            return defaultValue;
        }
        return val;
    }

    public Header getHeader(String requiredResource) {
        return getHeader(requiredResource, null);
    }

    public Header getHeader(String requiredResources, Set<String> requiredScopes) {
        return new Header("Authorization", "Bearer " + getAccessToken(requiredResources, requiredScopes));
    }


    public RequestSpecification getRequest(){
        RequestSpecification requestSpecification  = given().log().ifValidationFails();
        if (authType.equals(AuthType.OAUTH2)) {
            return requestSpecification.auth().oauth2(getAccessToken(resourceUrl.endsWith("/") ? resourceUrl : resourceUrl + "/"  ,null));
        } else {
            return requestSpecification;
        }
    }

    public RequestSpecification getUnauthenticatedRequest(){
        return given().log().ifValidationFails();
    }

    public RequestSpecification getJsonRequest(){
        return getRequest().accept(ContentType.JSON);
    }

    private String getAccessToken(String requiredResources, Set<String> requiredScopes) {
        Objects.requireNonNull(requiredResources);

        RequestSpecification specification = new RequestSpecBuilder()
            .setBaseUri(tokenUri)
            .build();

        //formatter:off
        RequestSpecification request = given(specification)
            .contentType("application/x-www-form-urlencoded")
            .formParam("grant_type", "client_credentials")
            .formParam("client_id", clientId)
            .formParam("client_secret", clientSecret);

        if (!requiredResources.isBlank()) {
            request.formParam("resource", requiredResources);
        }
        if (requiredScopes != null && !requiredScopes.isEmpty()) {
            request.formParam("scope", String.join(" ", requiredScopes));
        }

        return request.auth()
            .basic(clientId, clientSecret)
            .post()
            .then()
            .log().ifValidationFails(LogDetail.ALL)
            .assertThat()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getString("access_token");
        //formatter:on
    }

}
