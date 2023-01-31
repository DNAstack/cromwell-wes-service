package com.dnastack.wes.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "wes.auth")
public class AuthConfig {

    /**
     * The list of trusted token issuers for incoming requests. All Tokens must have originated at at least one of these
     * configured issues. Validity of the token is determined by a 1) Validity of the JWT (time), 2) Validity of the
     * issuerUri in the JWT header 3) validity of the audience if it is present in the issuerConfig 4) validity of the
     * configured scopes, if they are present in the issuer config
     */
    List<IssuerConfig> tokenIssuers = new ArrayList<>();


    @Getter
    @Setter
    public static class IssuerConfig {

        /**
         * The issuerUri this configuration applies to
         */
        String issuerUri;

        /**
         * The audience to test the JWT against. If this is set, the JWT must be issued for the appropriate audiences
         */
        List<String> audiences = new ArrayList<>();

        List<String> resources = new ArrayList<>();

        /**
         * The required scopes to test the JWT against. If this is set the JWT must have one of the valid scopes
         */
        List<String> scopes = new ArrayList<>();

        /**
         * URI to fetch the JSON Web Key set from. This key set should provide the public keys to verify the supplied
         * bearer token
         */
        String jwkSetUri = null;

        /**
         * RSA public key to use to verify the JWTs.
         */
        String rsaPublicKey = null;


    }

}
