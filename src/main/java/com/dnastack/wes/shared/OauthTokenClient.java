package com.dnastack.wes.shared;

import feign.Headers;
import feign.RequestLine;
import org.springframework.http.MediaType;

public interface OauthTokenClient {

    @RequestLine("POST")
    @Headers("Content-Type: " + MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    AccessToken getToken(OAuthRequest request);

}
