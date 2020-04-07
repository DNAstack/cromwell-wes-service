package com.dnastack.wes.service;

import static org.junit.jupiter.api.Assertions.fail;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;

public abstract class BaseE2eTest {

    @BeforeAll
    public static void setUpAllForAllTests() throws Exception {
        RestAssured.baseURI = optionalEnv("E2E_BASE_URI", "http://localhost:8090");
    }

    public static String requiredEnv(String name) {
        String val = System.getenv(name);
        if (val == null) {
            fail("Environnment variable `" + name + "` is required");
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
