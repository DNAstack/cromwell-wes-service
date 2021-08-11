package com.dnastack.wes.cromwell;

import lombok.*;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class CromwellId {

    private String id;
    private String status;

}
