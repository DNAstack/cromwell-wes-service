package com.dnastack.wes.transfer;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Builder
public class TrackedTransfer {

    @JsonProperty("last_update")
    public ZonedDateTime lastUpdate;
    @JsonProperty("created")
    public ZonedDateTime created;
    @JsonProperty("cromwell_id")
    private String cromwellId;
    @JsonProperty("transfer_job_ids")
    private List<String> transferJobIds;
    @JsonProperty("failure_attempts")
    private int failureAttempts;

}
