package com.dnastack.wes.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.KeyConverter;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.Key;
import java.security.PublicKey;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import javax.crypto.SecretKey;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;

public class PublicKeyJWKSelector<C extends SecurityContext> implements JWSKeySelector<C> {

    private final JWKSource jwkSource;
    private final JWSAlgorithm jwsAlg;

    public PublicKeyJWKSelector(final JWKSource<C> jwkSource) {
        Objects.requireNonNull(jwkSource);
        this.jwkSource = jwkSource;
        this.jwsAlg = JWSAlgorithm.parse(JwsAlgorithms.RS256);
    }

    private JWKMatcher createJWKMatcher(JWSHeader jwsHeader) {
        return new JWKMatcher.Builder()
            .keyType(KeyType.forAlgorithm(jwsAlg))
            .algorithms(jwsAlg, null)
            .x509CertSHA256Thumbprint(jwsHeader.getX509CertSHA256Thumbprint())
            .build();
    }

    @Override
    public List<? extends Key> selectJWSKeys(JWSHeader jwsHeader, C context) throws KeySourceException {
        if (!jwsAlg.equals(jwsHeader.getAlgorithm())) {
            // Unexpected JWS alg
            return Collections.emptyList();
        }

        JWKMatcher jwkMatcher = createJWKMatcher(jwsHeader);
        if (jwkMatcher == null) {
            return Collections.emptyList();
        }

        List<JWK> jwkMatches = jwkSource.get(new JWKSelector(jwkMatcher), context);

        List<Key> sanitizedKeyList = new LinkedList<>();

        for (Key key : KeyConverter.toJavaKeys(jwkMatches)) {
            if (key instanceof PublicKey || key instanceof SecretKey) {
                sanitizedKeyList.add(key);
            } // skip asymmetric private keys
        }

        return sanitizedKeyList;

    }
}
