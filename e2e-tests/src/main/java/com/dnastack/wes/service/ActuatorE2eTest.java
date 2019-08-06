package com.dnastack.wes.service;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


@DisplayName("Actuator tests")
public class ActuatorE2eTest extends BaseE2eTest {

  @Test
  @DisplayName("App information is appropriately returned")
  public void appNameAndVersionShouldBeExposed() {
    //@formatter:off
        given()
            .log().method()
            .log().uri()
        .when()
            .get("/actuator/info")
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body("app.name", equalTo("wes-service"))
            .body("app.version", notNullValue());
        //@formatter:on
  }

  @Test
  @DisplayName("Sensitive system information should not be exposed to users")
  public void sensitiveInfoShouldNotBeExposed() {
    Stream.of("auditevents", "beans", "conditions", "configprops", "env", "flyway", "httptrace", "logfile", "loggers",
        "liquibase", "metrics", "mappings", "prometheus", "scheduledtasks", "sessions", "shutdown", "threaddump")
        //@formatter:off
                .forEach(endpoint -> {
                    given()
                        .log().method()
                        .log().uri()
                    .when()
                        .get("/actuator/" + endpoint)
                    .then()
                        .log().ifValidationFails()
                        .statusCode(anyOf(equalTo(401), equalTo(404),equalTo(403)));
                    });
        //@formatter:on
  }

}