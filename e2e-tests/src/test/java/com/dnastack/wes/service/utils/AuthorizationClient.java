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
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.hamcrest.CoreMatchers;

public class AuthorizationClient {

    private final JWSSigner jwsSigner;
    private final String subject;

    public AuthorizationClient(String publicKey, String privateKey) throws JOSEException, InvalidKeySpecException, NoSuchAlgorithmException {
        assertThat(publicKey, CoreMatchers.notNullValue());
        assertThat(privateKey, CoreMatchers.notNullValue());

        RSAKey rsaKey = createRSAKey(sanitizeKeyString(publicKey), sanitizeKeyString(privateKey));
        jwsSigner = new RSASSASigner(rsaKey);
        subject = UUID.randomUUID().toString();
    }


    private RSAKey createRSAKey(String publicKeyContent, String privateKeyContent) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec x509EncodedKeySpec =
            new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent));
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec =
            new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyContent));
        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(pkcs8EncodedKeySpec);
        RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(x509EncodedKeySpec);
        return new RSAKey.Builder(publicKey).privateKey(privateKey).build();
    }

    private String sanitizeKeyString(String keyString) {
        return keyString.replaceAll("\\n", "").replaceAll("\\\\n","").replaceAll("-----BEGIN (PUBLIC|PRIVATE) "
                + "KEY-----",
            "").replaceAll("-----END (PUBLIC|PRIVATE) KEY-----", "");
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
