package com.dnastack.wes.config;

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
     * The issuer URI is the audience that is expected to be in the JWT and will be used for verifying the access token
     */
    String issuerUri;

    /**
     * URI to fetch the JSON Web Key set from. This key set should provide the public keys to verify the supplied bearer
     * token
     */
    String jwkSetUri = null;

    /**
     * RSA public key to use to verify the JWTs.
     */
    String rsaPublicKey = null;

    /**
     * Attribute name of the principal within the JWT
     */
    String userNameAttribute;

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
}
