package com.dnastack.wes.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;


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

    @JsonProperty(value = "id")
    String id;
    @JsonProperty(value = "cmd")
    String cmd;

    @JsonProperty(value = "start_time")
    Instant startTime;

    @JsonProperty(value = "end_time")
    Instant endTime;

    @JsonProperty(value = "stdout")
    String stdout;

    @JsonProperty(value = "stderr")
    String stderr;

    @JsonProperty(value = "exit_code")
    Integer exitCode;


}
