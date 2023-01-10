package com.dnastack.wes.cromwell;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Builder
public class CromwellSearch {

    private List<String> id;

    private String start;

    private String end;

    private String name;

    private List<String> status;

    private List<String> label;

    private Integer page;

    private Integer pageSize;

}
