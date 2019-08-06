package com.dnastack.wes.service.utils;

import static org.hamcrest.MatcherAssert.assertThat;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.restassured.http.Header;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.UUID;
import org.hamcrest.CoreMatchers;

public class AuthorizationClient {

    private final JWSSigner jwsSigner;
    private final String subject;

    public AuthorizationClient(String privateKeyContent) throws JOSEException {
        assertThat(privateKeyContent, CoreMatchers.notNullValue());
        KeyPair keyPair = RsaKeyHelper.parseKeyPair(privateKeyContent);
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAKey rsaKey = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        jwsSigner = new RSASSASigner(rsaKey);
        subject = UUID.randomUUID().toString();
    }


    public String getAccessToken() {

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().audience("wes-e2e-test").issuer("https://testing.dnastack"
            + ".com").subject(subject).expirationTime(new Date(new Date().getTime() + (60 * 1000))).build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build();
        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        try {
            signedJWT.sign(jwsSigner);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new AssertionError(e);
        }
    }

    public Header getHeader() {
        return new Header("Authorization", "Bearer " + getAccessToken());
    }

}
