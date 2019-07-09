package com.dnastack.wes.service.utils;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.Header;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import java.net.URI;


public class Oauth2Client {


    private String accessToken;
    private String refreshToken;
    private String client;
    private String username;
    private String password;

    private Long tokenGrantTime;
    private Long tokenExpiresIn;
    private RequestSpecification requestSpecification;

    public Oauth2Client(String tokenUrl, String client, String username, String password) {
        this.client = client;
        this.username = username;
        this.password = password;
        URI tokenUri = URI.create(tokenUrl);

        requestSpecification = new RequestSpecBuilder().setBaseUri(tokenUri).build();

    }

    private RequestSpecification getRequest() {
        return RestAssured.given().spec(requestSpecification);
    }


    public String getAccessToken() {
        if (accessToken == null) {
            return grantToken();
        } else if (tokenExpired()) {
            return grantRefreshToken();
        } else {
            return accessToken;
        }
    }

    public Header getHeader() {
        return new Header("Authorization", "Bearer " + getAccessToken());
    }

    private String grantToken() {
        tokenGrantTime = System.currentTimeMillis() / 1000;
        //@formatter:off
        JsonPath json = getRequest()
                .log().uri()
                .log().method()
                .formParam("client_id", client)
                .formParam("grant_type", "password")
                .formParam("username", username)
                .formParam("password", password)
            .post()
            .then()
                .assertThat()
                .statusCode(200)
                .extract()
                .jsonPath();
        //@formatter:on
        accessToken = json.getString("access_token");
        refreshToken = json.getString("refresh_token");
        tokenExpiresIn = json.getLong("expires_in");
        return accessToken;
    }

    private String grantRefreshToken() {
        tokenGrantTime = System.currentTimeMillis() / 1000;
        //@formatter:off
        JsonPath json = getRequest()
                .formParam("client_id", client)
                .formParam("grant_type", "refresh_token")
                .formParam("refresh_token", refreshToken)
            .post()
                .then()
                .extract()
                .jsonPath();
        //@formatter:on

        accessToken = json.getString("access_token");
        refreshToken = json.getString("refresh_token");
        tokenExpiresIn = json.getLong("expires_in");

        return accessToken;
    }

    private Boolean tokenExpired() {
        return !((System.currentTimeMillis() / 1000) > (tokenGrantTime + tokenExpiresIn));
    }
}
