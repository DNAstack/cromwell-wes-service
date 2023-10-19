package com.dnastack.wes.api;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    public enum FileType {
        FINAL,
        SECONDARY,
        LOG
    }

}
