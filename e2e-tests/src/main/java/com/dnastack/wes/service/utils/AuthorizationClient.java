package com.dnastack.wes.service.utils;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;

import java.util.Objects;
import java.util.Set;

import static com.dnastack.wes.service.BaseE2eTest.optionalEnv;
import static com.dnastack.wes.service.BaseE2eTest.requiredEnv;
import static io.restassured.RestAssured.given;

public class AuthorizationClient {

    private static String tokenUri = requiredEnv("E2E_TOKEN_URI");
    private static String clientId = requiredEnv("E2E_CLIENT_ID");
    private static String clientSecret = requiredEnv("E2E_CLIENT_SECRET");

    private static String audience = optionalEnv("E2E_CLIENT_AUDIENCE", null);
    private static String scopes = optionalEnv("E2E_CLIENT_SCOPES", null);
    private static String resources = optionalEnv("E2E_CLIENT_RESOURCES", null);

    private String getAccessToken() {
        RequestSpecification specification = new RequestSpecBuilder()
            .setBaseUri(tokenUri)
            .build();

        //formatter:off
        RequestSpecification request = given(specification)
            .contentType("application/x-www-form-urlencoded")
            .formParam("grant_type", "client_credentials")
            .formParam("client_id", clientId)
            .formParam("client_secret", clientSecret);
        if (audience != null) {
            request.formParam("audience", audience);
        }
        if (scopes != null) {
            request.formParam("scope", scopes);
        }
        if (resources != null) {
            request.formParam("resource", resources);
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

    private String getAccessToken(String requiredResources, Set<String> requiredScopes) {
        Objects.requireNonNull(requiredResources);
        Objects.requireNonNull(requiredScopes);

        RequestSpecification specification = new RequestSpecBuilder()
                .setBaseUri(tokenUri)
                .build();

        //formatter:off
        RequestSpecification request = given(specification)
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "client_credentials")
                .formParam("client_id", clientId)
                .formParam("client_secret", clientSecret);

        if (audience != null) {
            request.formParam("audience", audience);
        }
        if (!requiredResources.isBlank()) {
            request.formParam("resource", requiredResources);
        }
        if (!requiredScopes.isEmpty()) {
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

    public Header getHeader() {
        return new Header("Authorization", "Bearer " + getAccessToken());
    }

    public Header getHeader(String requiredResources, Set<String> requiredScopes) {
        return new Header("Authorization", "Bearer " + getAccessToken(requiredResources, requiredScopes));
    }

}
