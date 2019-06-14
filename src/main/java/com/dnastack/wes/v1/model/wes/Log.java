package com.dnastack.wes.v1.model.wes;

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
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class Log {

    @JsonProperty(value = "name")
    String name;

    @JsonProperty(value = "cmd")
    String cmd;

    @JsonProperty(value = "start_time")
    String startTime;

    @JsonProperty(value = "end_time")
    String endTime;

    @JsonProperty(value = "stdout")
    String stdout;

    @JsonProperty(value = "stderr")
    String stderr;

    @JsonProperty(value = "exit_code")
    Integer exitCode;


}
