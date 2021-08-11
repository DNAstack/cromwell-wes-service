package com.dnastack.wes.drs;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class AccessURL {

    List<String> headers;
    String url;

}
