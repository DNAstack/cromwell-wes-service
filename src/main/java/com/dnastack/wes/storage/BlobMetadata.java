package com.dnastack.wes.storage;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BlobMetadata {

    private String name;
    private String contentType;
    private String contentEncoding;
    private long size;
    private Instant creationTime;
    private Instant lastModifiedTime;
    private List<Checksum> checksums;



    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Checksum {

        private ChecksumType type;
        private String value;

    }

    public enum ChecksumType {
        MD5,
        CRC32
    }
}
