package com.dnastack.wes.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredentialsModel {

    private String accessKeyId;
    private String accessToken;
    private String sessionToken;

}
