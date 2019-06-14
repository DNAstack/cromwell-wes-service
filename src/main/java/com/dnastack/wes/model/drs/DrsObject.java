package com.dnastack.wes.model.drs;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
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
public class DrsObject {

    String created;
    String description;
    String id;

    @JsonProperty("mime_type")
    String mimeType;
    Long size;
    String updated;
    String version;
    List<String> aliases;

    @JsonProperty("check_sums")
    List<CheckSum> checkSums;

    @JsonProperty("access_methods")
    List<AccessMethod> accessMethods;

}
