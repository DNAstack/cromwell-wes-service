package com.dnastack.wes.service.utils;

import static com.dnastack.wes.service.BaseE2eTest.optionalEnv;
import static com.dnastack.wes.service.BaseE2eTest.requiredEnv;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.Header;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.UUID;

import io.restassured.specification.RequestSpecification;
import org.hamcrest.CoreMatchers;

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

    public Header getHeader() {
        return new Header("Authorization", "Bearer " + getAccessToken());
    }

}
