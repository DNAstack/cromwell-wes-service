package com.dnastack.wes.transfer;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
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
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Builder
public class TrackedTransfer {

    @JsonProperty("cromwell_id")
    private String cromwellId;

    @JsonProperty("transfer_job_ids")
    private List<String> transferJobIds;

    @JsonProperty("failure_attempts")
    private int failureAttempts;

    @JsonProperty("last_update")
    public ZonedDateTime lastUpdate;

    @JsonProperty("created")
    public ZonedDateTime created;

}
