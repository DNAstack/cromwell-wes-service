package com.dnastack.wes;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OauthConfig {

    String identityProvider;

    String oidcTokenUri;

    String serviceAccountClientId;

    String serviceAccountSecret;

}
