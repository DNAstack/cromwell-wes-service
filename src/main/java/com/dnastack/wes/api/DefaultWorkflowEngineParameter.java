package com.dnastack.wes.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
