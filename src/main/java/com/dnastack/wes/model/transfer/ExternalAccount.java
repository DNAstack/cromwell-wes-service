package com.dnastack.wes.model.transfer;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class ExternalAccount {

    private String externalAccountId;
    private String externalAccountName;
    private String authBaseURL;
    private Map<String, String> info;
    private String providerName;
    private String scopes;
    private String state;

}
