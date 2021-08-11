package com.dnastack.wes.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
@ToString
public class DefaultWorkflowEngineParameter {

    @JsonProperty("name")
    String name;

    @JsonProperty("type")
    String type;

    @JsonProperty("default_value")
    String defaultValue;

}
