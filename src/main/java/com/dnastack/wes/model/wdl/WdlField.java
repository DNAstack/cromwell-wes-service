package com.dnastack.wes.model.wdl;

import com.dnastack.wes.utils.WdlTypeRepresentation;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class WdlField {

    @JsonProperty("name")
    private String name;

    @JsonProperty("type_name")
    private String typeName;

    @JsonProperty("type")
    private WdlTypeRepresentation type;
}
