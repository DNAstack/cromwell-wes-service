package com.dnastack.wes.registry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@NoArgsConstructor
public class PublicAuthenticationConfiguration {

    private String accessTokenUrl;

    private String clientId;

    private String clientSecret;

    private String deviceCodeUrl;

    private String grantType;

    /**
     * Resource URL
     */
    private String resource;

    /**
     * Scopes split by whitespace, e.g., "scope_1 scope_2 ... scope_n"
     */
    private String scope;

    private String type;

    public String getResource() {
        if (!resource.endsWith("/")) {
            return resource + "/";
        } else {
            return resource;
        }
    }

}
