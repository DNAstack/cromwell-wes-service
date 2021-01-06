package com.dnastack.wes.service;

import static org.junit.jupiter.api.Assertions.fail;

import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.config.RestAssuredConfig;
import org.junit.jupiter.api.BeforeAll;

public abstract class BaseE2eTest {

    @BeforeAll
    public static void setUpAllForAllTests() throws Exception {
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
}
