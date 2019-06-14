package com.dnastack.wes.v1.model.wes;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
