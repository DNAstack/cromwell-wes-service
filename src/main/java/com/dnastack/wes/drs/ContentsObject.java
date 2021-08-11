package com.dnastack.wes.drs;

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
public class ContentsObject {


    String id;

    String name;

    @JsonProperty("drs_uri")
    List<String> drsUri;

    List<ContentsObject> contents;

}
