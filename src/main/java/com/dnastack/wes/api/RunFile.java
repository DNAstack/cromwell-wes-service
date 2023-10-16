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

    @JsonProperty(value = "type")
    Enum<type> type;

    @JsonProperty(value = "path")
    String path;

    public enum type {
        FINAL,
        SECONDARY,
        LOG
    }

}
