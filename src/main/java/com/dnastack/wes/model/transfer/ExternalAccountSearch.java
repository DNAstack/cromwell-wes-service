package com.dnastack.wes.model.transfer;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ExternalAccountSearch {


    private String userId;

    private String infoVal;

    private String infoKey;

    private Integer pageNumber;

    private Integer maxPerPage;

}
