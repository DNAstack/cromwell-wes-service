package com.dnastack.wes.service;

import static io.restassured.RestAssured.given;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Welcome Page Tests")
public class WelcomeE2ETest extends BaseE2eTest {


  @Test
  @DisplayName("Base URI should return 200 and HTML with Client side redirect")
  public void baseUriReturnsOkAndHtml() {
    //@formatter:off
    given()
        .log().uri()
        .log().method()
    .get()
    .then()
        .assertThat()
        .statusCode(200)
        .contentType("text/html")
        .content(Matchers.containsString("Health check page."));
    //@formatter:on
  }

}
