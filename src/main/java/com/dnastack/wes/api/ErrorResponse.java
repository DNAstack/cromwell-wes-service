package com.dnastack.wes.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ErrorResponse {

    @JsonProperty("msg")
    String msg;

    @JsonProperty("error_code")
    Integer errorCode;

}
