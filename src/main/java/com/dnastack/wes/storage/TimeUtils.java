package com.dnastack.wes.storage;

import java.time.Instant;
import java.time.OffsetDateTime;

public class TimeUtils {
    private TimeUtils(){

    }

    public static Instant offsetToInstant(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return null;
        }
        return offsetDateTime.toInstant();
    }
}
