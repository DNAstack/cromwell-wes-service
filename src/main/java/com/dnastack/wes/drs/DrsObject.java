package com.dnastack.wes.drs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class DrsObject {

    String id;

    @JsonProperty("created_time")
    String createdTime;

    @JsonProperty("updated_time")
    String updatedTime;

    String description;

    String name;

    Long size;

    @JsonProperty("self_uri")
    String selfUri;

    @JsonProperty("mime_type")
    String mimeType;

    String version;

    List<String> aliases;

    @JsonProperty("check_sums")
    List<CheckSum> checkSums;

    @JsonProperty("access_methods")
    List<AccessMethod> accessMethods;

    List<ContentsObject> contents;

    @JsonIgnore
    List<DrsObject> resolvedDrsContents;

}
