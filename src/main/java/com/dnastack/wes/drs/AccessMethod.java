package com.dnastack.wes.drs;

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
