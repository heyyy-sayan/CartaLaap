package com.cartalaap.auth;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.cartalaap.config.JwtProperties;
import com.cartalaap.user.User;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Service
public class TokenService {
    private final SecretKey secretKey;
    private final JwtProperties properties;

    public TokenService(SecretKey secretKey, JwtProperties properties) {
        this.secretKey = secretKey;
        this.properties = properties;
    }

    public IssuedToken issue(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.expiration());
        var claims = new JWTClaimsSet.Builder()
                .issuer("cartalaap-api")
                .subject(user.getUsername())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .jwtID(UUID.randomUUID().toString())
                .claim("uid", user.getId())
                .claim("roles", List.of(user.getRole().name()))
                .build();
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(new MACSigner(secretKey.getEncoded()));
            return new IssuedToken(jwt.serialize(), expiresAt);
        } catch (JOSEException exception) {
            throw new IllegalStateException("Could not create access token", exception);
        }
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}
