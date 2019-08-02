package com.dnastack.wes.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.RemoteKeySourceException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;
import org.springframework.util.Assert;

public class PublicKeyJwtDecoder implements JwtDecoder {

    private static final String DECODING_ERROR_MESSAGE_TEMPLATE =
        "An error occurred while attempting to decode the Jwt: %s";

    private final JWSAlgorithm jwsAlgorithm;
    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    private Converter<Map<String, Object>, Map<String, Object>> claimSetConverter =
        MappedJwtClaimSetConverter.withDefaults(Collections.emptyMap());
    private OAuth2TokenValidator<Jwt> jwtValidator = JwtValidators.createDefault();

    public PublicKeyJwtDecoder(String publicKeyContent){
        this(publicKeyContent, JwsAlgorithms.RS256);
    }

    public PublicKeyJwtDecoder(String publicKeyContent, String algorithm) {
        Assert.hasText(publicKeyContent, "publicKeyContent cannot be empty or null");
        Assert.hasText(algorithm, "algorithm cannot be null");

        JWKSource jwkSource;
        try {
            publicKeyContent = publicKeyContent.replaceAll("\\n", "").replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "");
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec x509EncodedKeySpec =
                new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent));
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(x509EncodedKeySpec);
            RSAKey key = new RSAKey.Builder(publicKey).build();
            jwkSource = new ImmutableJWKSet(new JWKSet(key));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalArgumentException("Invalid publicKeyContent: " + e.getMessage(), e);
        }

        this.jwsAlgorithm = JWSAlgorithm.parse(algorithm);
        JWSKeySelector<SecurityContext> jwsKeySelector =
            new PublicKeyJWKSelector<>(jwkSource);
        this.jwtProcessor = new DefaultJWTProcessor<>();
        this.jwtProcessor.setJWSKeySelector(jwsKeySelector);

        // Spring Security validates the claim set independent from Nimbus
        this.jwtProcessor.setJWTClaimsSetVerifier((claims, context) -> {
        });
    }


    @Override
    public Jwt decode(String token) throws JwtException {
        JWT jwt = this.parse(token);
        if (jwt instanceof SignedJWT) {
            Jwt createdJwt = this.createJwt(token, jwt);
            return this.validateJwt(createdJwt);
        }
        throw new JwtException("Unsupported algorithm of " + jwt.getHeader().getAlgorithm());
    }

    /**
     * Use this {@link Jwt} Validator
     *
     * @param jwtValidator - the Jwt Validator to use
     */
    public void setJwtValidator(OAuth2TokenValidator<Jwt> jwtValidator) {
        Assert.notNull(jwtValidator, "jwtValidator cannot be null");
        this.jwtValidator = jwtValidator;
    }

    /**
     * Use the following {@link Converter} for manipulating the JWT's claim set
     *
     * @param claimSetConverter the {@link Converter} to use
     */
    public final void setClaimSetConverter(Converter<Map<String, Object>, Map<String, Object>> claimSetConverter) {
        Assert.notNull(claimSetConverter, "claimSetConverter cannot be null");
        this.claimSetConverter = claimSetConverter;
    }

    private JWT parse(String token) {
        try {
            return JWTParser.parse(token);
        } catch (Exception ex) {
            throw new JwtException(String.format(DECODING_ERROR_MESSAGE_TEMPLATE, ex.getMessage()), ex);
        }
    }

    private Jwt createJwt(String token, JWT parsedJwt) {
        Jwt jwt;

        try {
            // Verify the signature
            JWTClaimsSet jwtClaimsSet = this.jwtProcessor.process(parsedJwt, null);

            Map<String, Object> headers = new LinkedHashMap<>(parsedJwt.getHeader().toJSONObject());
            Map<String, Object> claims = this.claimSetConverter.convert(jwtClaimsSet.getClaims());

            Instant expiresAt = (Instant) claims.get(JwtClaimNames.EXP);
            Instant issuedAt = (Instant) claims.get(JwtClaimNames.IAT);
            jwt = new Jwt(token, issuedAt, expiresAt, headers, claims);
        } catch (RemoteKeySourceException ex) {
            if (ex.getCause() instanceof ParseException) {
                throw new JwtException(String.format(DECODING_ERROR_MESSAGE_TEMPLATE, "Malformed Jwk set"));
            } else {
                throw new JwtException(String.format(DECODING_ERROR_MESSAGE_TEMPLATE, ex.getMessage()), ex);
            }
        } catch (Exception ex) {
            if (ex.getCause() instanceof ParseException) {
                throw new JwtException(String.format(DECODING_ERROR_MESSAGE_TEMPLATE, "Malformed payload"));
            } else {
                throw new JwtException(String.format(DECODING_ERROR_MESSAGE_TEMPLATE, ex.getMessage()), ex);
            }
        }

        return jwt;
    }

    private Jwt validateJwt(Jwt jwt) {
        OAuth2TokenValidatorResult result = this.jwtValidator.validate(jwt);
        if (result.hasErrors()) {
            String description = result.getErrors().iterator().next().getDescription();
            throw new JwtValidationException(
                String.format(DECODING_ERROR_MESSAGE_TEMPLATE, description),
                result.getErrors());
        }

        return jwt;
    }
}
