package app.auth.cognito.interceptor.cognito;

import app.auth.cognito.module.data.CognitoClientCredentialsSecret;
import app.auth.cognito.module.data.CognitoStackOutputs;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.Test;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class CognitoServerInterceptorTest {
    private static final String USER_POOL_ID = "pool";
    private static final String CLIENT_ID = "client";
    private static final String REGION = "us-east-1";

    private static class TestInterceptor extends CognitoServerInterceptor {
        private final PublicKey key;
        TestInterceptor(PublicKey key) {
            super(new CognitoStackOutputs(USER_POOL_ID, "redir", "arn", "base", "signin"),
                  new CognitoClientCredentialsSecret(CLIENT_ID, "secret"),
                  REGION,
                  Logger.getLogger("test"));
            this.key = key;
        }

        @Override
        protected Optional<PublicKey> getPublicKey(String kid, String userPoolId, String region) {
            return Optional.of(key);
        }
    }

    private String createToken(PrivateKey privateKey, Date exp, String aud) {
        return Jwts.builder()
                   .setHeaderParam("kid", "kid")
                   .setSubject("sub")
                   .claim("email", "e@x.com")
                   .claim("name", "name")
                   .setAudience(aud)
                   .setExpiration(exp)
                   .signWith(privateKey, SignatureAlgorithm.RS256)
                   .compact();
    }

    @Test
    public void authenticateUser_rejectsExpired() {
        KeyPair kp = Keys.keyPairFor(SignatureAlgorithm.RS256);
        CognitoServerInterceptor interceptor = new TestInterceptor(kp.getPublic());

        String token = createToken(kp.getPrivate(), new Date(System.currentTimeMillis() - 1000), CLIENT_ID);
        assertTrue(interceptor.authenticateUser(token).isEmpty());
    }

    @Test
    public void authenticateUser_rejectsWrongAudience() {
        KeyPair kp = Keys.keyPairFor(SignatureAlgorithm.RS256);
        CognitoServerInterceptor interceptor = new TestInterceptor(kp.getPublic());

        String token = createToken(kp.getPrivate(), new Date(System.currentTimeMillis() + 10000), "other");
        assertTrue(interceptor.authenticateUser(token).isEmpty());
    }

    @Test
    public void authenticateUser_acceptsValid() {
        KeyPair kp = Keys.keyPairFor(SignatureAlgorithm.RS256);
        CognitoServerInterceptor interceptor = new TestInterceptor(kp.getPublic());

        String token = createToken(kp.getPrivate(), new Date(System.currentTimeMillis() + 10000), CLIENT_ID);
        assertTrue(interceptor.authenticateUser(token).isPresent());
    }
}
