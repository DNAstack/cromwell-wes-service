package com.dnastack.wes.drs;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class CheckSum {

    String checksum;

    String type;

}
