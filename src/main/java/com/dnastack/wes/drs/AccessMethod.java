package com.dnastack.wes.drs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class AccessMethod {

    @JsonProperty("access_id")
    String accessId;

    @JsonProperty("access_url")
    AccessURL accessUrl;

    String region;

    AccessType type;

}
