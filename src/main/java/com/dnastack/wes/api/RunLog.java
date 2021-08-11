package com.dnastack.wes.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
@ToString
@Builder
public class RunLog {

    @JsonProperty("run_id")
    String runId;

    @JsonProperty("request")
    RunRequest request;

    @JsonProperty("state")
    State state;

    @JsonProperty("run_log")
    Log runLog;

    @JsonProperty("task_logs")
    List<Log> taskLogs;

    @JsonProperty("outputs")
    Map<String, Object> outputs;

}
