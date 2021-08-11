package com.dnastack.wes.cromwell;

import lombok.*;

import java.time.ZonedDateTime;

/**
 * Status object received from the cromwell Query REST API for a singl
 *
 * @author Patrick Magee
 * created on: 2018-08-21
 */
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CromwellStatus {

    private String id;

    private String name;

    private ZonedDateTime start;

    private ZonedDateTime end;

    private String status;

}
