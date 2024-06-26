package com.dnastack.wes.service.util;

import static org.junit.jupiter.api.Assertions.fail;

public final class EnvUtil {

    private EnvUtil() {
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

    public static <T> T optionalEnv(String name, T defaultValue,  Converter<T> converter) {
        String val = System.getenv(name);
        if (val == null) {
            return defaultValue;
        }
        return converter.convert(val);
    }

    @FunctionalInterface
    public interface Converter<T> {
        T convert(String value);
    }

}
