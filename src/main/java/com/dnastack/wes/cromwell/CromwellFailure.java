package com.dnastack.wes.cromwell;

import lombok.*;

import java.util.List;

/**
 * @author Patrick Magee
 * created on: 2018-09-24
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class CromwellFailure {

    private String message;
    private List<CromwellFailure> causedBy;

}
