package com.dnastack.wes.cromwell;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Response object received from the cromwell Query REST api
 *
 * @author Patrick Magee
 * created on: 2018-08-21
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class CromwellResponse {

    private Long totalResultsCount;

    private List<CromwellStatus> results;

}
