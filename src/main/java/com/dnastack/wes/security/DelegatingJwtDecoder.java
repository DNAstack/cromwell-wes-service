package com.dnastack.wes.security;

import com.dnastack.wes.config.AuthConfig.IssuerConfig;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoderJwkSupport;
import org.springframework.util.Assert;

@Slf4j
public class DelegatingJwtDecoder implements JwtDecoder {

    private static final String DECODING_ERROR_MESSAGE_TEMPLATE =
        "An error occurred while attempting to decode the Jwt: %s";
    private final Map<String, JwtDecoder> delegates;

    public DelegatingJwtDecoder(List<IssuerConfig> issuers) {
        Assert.notEmpty(issuers, "Must supply at least one issuer");
        delegates = buildJwtDecoders(issuers);
    }

    private Map<String, JwtDecoder> buildJwtDecoders(List<IssuerConfig> issuers) {
        Map<String, JwtDecoder> contexts = new HashMap<>();
        for (IssuerConfig issuerConfig : issuers) {
            OAuth2TokenValidator<Jwt> oauthValidator = createOauthValidater(issuerConfig);
            JwtDecoder decoder;
            if (issuerConfig.getJwkSetUri() != null) {
                NimbusJwtDecoderJwkSupport nimbusJwtDecoderJwkSupport =
                    new NimbusJwtDecoderJwkSupport(issuerConfig.getJwkSetUri());
                nimbusJwtDecoderJwkSupport.setJwtValidator(oauthValidator);
                decoder = nimbusJwtDecoderJwkSupport;
            } else if (issuerConfig.getRsaPublicKey() != null) {
                PublicKeyJwtDecoder publicKeyJwtDecoder = new PublicKeyJwtDecoder(issuerConfig.getRsaPublicKey());
                publicKeyJwtDecoder.setJwtValidator(oauthValidator);
                decoder = publicKeyJwtDecoder;
            } else {
                throw new IllegalArgumentException(String.format("Invalid Issuer config for issuer: %s. neither "
                    + "jwkSetUri or rsaPublicKey is set", issuerConfig.getIssuerUri()));
            }

            contexts.put(issuerConfig.getIssuerUri(), decoder);
        }
        return Collections.unmodifiableMap(contexts);
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        JWT jwt = this.parse(token);
        String issuer;
        try {
            issuer = jwt.getJWTClaimsSet().getIssuer();
        } catch (ParseException e) {
            throw new JwtException(e.getMessage(), e);
        }

        JwtDecoder delegate = delegates.get(issuer);
        if (delegate == null) {
            throw new JwtException("No configured JWT decoders for issuer " + issuer);
        }
        return delegate.decode(token);
    }

    private JWT parse(String token) {
        try {
            return JWTParser.parse(token);
        } catch (Exception ex) {
            throw new JwtException(String.format(DECODING_ERROR_MESSAGE_TEMPLATE, ex.getMessage()), ex);
        }
    }

    private static OAuth2TokenValidator<Jwt> createOauthValidater(IssuerConfig issuerConfig) {
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtTimestampValidator());
        validators.add(new JwtIssuerValidator(issuerConfig.getIssuerUri()));
        List<String> aud = issuerConfig.getAudiences();
        if (aud != null && !aud.isEmpty()) {
            validators.add(new JwtAudienceValidator(aud));
        }
        return new DelegatingOAuth2TokenValidator<>(validators);
    }


}
