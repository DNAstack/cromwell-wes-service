package com.dnastack.wes.files;

import com.dnastack.wes.storage.BlobMetadata;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RunFile {

    private FileType fileType;
    private String path;
    @JsonUnwrapped
    private BlobMetadata blobMetadata;


    public RunFile(FileType fileType, String path) {
        this.fileType = fileType;
        this.path = path;
    }


    public enum FileType {
        FINAL,
        SECONDARY,
        LOG,
        INPUT
    }

}
