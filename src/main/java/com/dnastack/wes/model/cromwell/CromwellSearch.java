package com.dnastack.wes.model.cromwell;

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

    private Long page;

    private Long pageSize;
}
