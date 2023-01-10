package com.dnastack.wes.translation;

import lombok.*;

import java.util.Map;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Builder
public class OriginalInputs {

    String id;

    Map<String, Object> mapping;

}
