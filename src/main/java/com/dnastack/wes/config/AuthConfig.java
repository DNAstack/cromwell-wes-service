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
    @Deprecated
    String serviceAccountAuthenticationUri;

    /**
     * The service account client id which will be used to authenticate this service against others
     */
    @Deprecated
    String serviceAccountClientId;

    /**
     * The service account client secret which will be used to authenticate this service against others
     */
    @Deprecated
    String serviceAccountSecret;


    /**
     * TODO Remove this The development token issuer.
     */
    @Deprecated
    IssuerConfig devTokenIssuer = null;

    /**
     * The list of trusted token issuers for incoming requests. All Tokens must have orignated at at least one of these
     * configured issues. Validity of the token is determined by a 1) Validity of the JWT (time), 2) Validity of the
     * issuerUri in the JWT header 3) validity of the audiene if it is present in the issuerConfig 4) validity of the
     * configured scopes, if they are pressent in the issuer config
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
