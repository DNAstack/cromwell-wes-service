package com.dnastack.wes.files;

import com.dnastack.wes.storage.BlobMetadata;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class RunFile {

    @JsonProperty(value = "file_type")
    FileType fileType;
    String path;
    @JsonUnwrapped
    BlobMetadata blobMetadata;


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
