package com.dnastack.wes.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "wes.auth")
public class AuthConfig {

    /**
     * The URI to use for authenticated service to service communication. This is expected to be an OIDC compliant token
     * endpoint which will accept {@code client_credentials}
     */
    String serviceAccountAuthenticationUri;

    /**
     * The service account client id which will be used to authenticate this service against others
     */
    String serviceAccountClientId;

    /**
     * The service account client secret which will be used to authenticate this service against others
     */
    String serviceAccountSecret;


    IssuerConfig devTokenIssuer = null;

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
