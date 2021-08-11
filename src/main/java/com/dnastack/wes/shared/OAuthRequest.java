package com.dnastack.wes.shared;

import feign.form.FormProperty;
import lombok.Data;

@Data
public class OAuthRequest {

    @FormProperty("grant_type")
    String grantType;

    @FormProperty("username")
    String username;

    @FormProperty("password")
    String password;

    @FormProperty("client_id")
    String clientId;

    @FormProperty("client_secret")
    String clientSecret;

    @FormProperty("resource")
    String resource;

}
