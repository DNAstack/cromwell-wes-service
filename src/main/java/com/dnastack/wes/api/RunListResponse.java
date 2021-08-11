package com.dnastack.wes.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
@ToString
@Builder
public class RunListResponse {

    @JsonProperty("runs")
    List<RunStatus> runs;


    @JsonProperty("next_page_token")
    String nextPageToken;

}
