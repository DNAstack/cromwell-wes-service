package com.dnastack.wes.utils;

import com.dnastack.wes.api.RangeNotSatisfiableException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;

import java.util.List;

public class RangeHeaderUtils {
    private RangeHeaderUtils(){

    }

    public static HttpRange getRangeFromHeaders(HttpServletResponse response, HttpHeaders headers) {
        List<HttpRange> ranges = headers.getRange();
        if (ranges.isEmpty()) {
            return null;
        } else if (ranges.size() > 1) {
            // only return the first range parsed
            throw new RangeNotSatisfiableException("Streaming of multiple ranges is not supported");
        } else {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            return ranges.get(0);
        }
    }
}
