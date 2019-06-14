package com.dnastack.wes.model.wes;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ErrorResponse {

    @JsonProperty("msg")
    String msg;

    @JsonProperty("error_code")
    Integer errorCode;
}
