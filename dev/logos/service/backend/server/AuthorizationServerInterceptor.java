package dev.logos.service.backend.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;


abstract class User {
    public String getId() {
        throw new UnsupportedOperationException();
    }

    public boolean isAuthenticated() {
        return false;
    }
}

class AuthenticatedUser extends User {
    private final String id;

    public AuthenticatedUser(Claims claims) {
        this.id = claims.get("username", String.class);
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }
}

public class AuthorizationServerInterceptor implements ServerInterceptor {
    public static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of("Authorization",
                                                                                          ASCII_STRING_MARSHALLER);
    private static final String COGNITO_IDENTITY_POOL_URL_TEMPLATE = "https://cognito-idp.{region}.amazonaws.com/{userPoolId}/.well-known/jwks.json";
    private static final Map<String, Key> keyCache = new ConcurrentHashMap<>();
    private static final Logger logger = Logger.getLogger(AuthorizationServerInterceptor.class.getName());
    private static final Context.Key<User> USER_CONTEXT_KEY = Context.key("user");
    public static final String BEARER_TYPE = "Bearer";

    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            final Metadata requestHeaders,
            ServerCallHandler<ReqT, RespT> next
    ) {
        String value = requestHeaders.get(AUTHORIZATION_METADATA_KEY);
        Context ctx = Context.current();
        User user = null;

        if (value != null && value.startsWith(BEARER_TYPE)) {
            String token = value.substring(BEARER_TYPE.length()).trim();
            user = authenticateUser(token, "us-east-2_0tayqImgc", "us-east-2");
        }

        return Contexts.interceptCall(ctx.withValue(USER_CONTEXT_KEY, user), call, requestHeaders, next);
    }

    private AuthenticatedUser authenticateUser(String token, String userPoolId, String region) {
        try {
            String headerJson = new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]),
                                           StandardCharsets.UTF_8);
            Map<String, String> headerMap = new ObjectMapper().readValue(headerJson, new TypeReference<>() {});
            String kid = headerMap.get("kid");

            Jws<Claims> claims = Jwts.parser()
                                     .setSigningKey(getPublicKey(kid, userPoolId, region))
                                     .build()
                                     .parseClaimsJws(token);

            claims.getPayload().forEach((key, value) -> logger.info(key + ": " + value));

            return new AuthenticatedUser(claims.getPayload());
        } catch (Exception e) {
            logger.warning("Failed to authenticate: " + e.getMessage());
            return null;
        }
    }

    private static Key getPublicKey(String kid, String userPoolId, String region) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        Key cachedKey = keyCache.get(kid);
        if (cachedKey != null) {
            return cachedKey;
        }

        String url = COGNITO_IDENTITY_POOL_URL_TEMPLATE.replace("{region}", region).replace("{userPoolId}", userPoolId);
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jwkMap = objectMapper.readValue(new URL(url), new TypeReference<Map<String, Object>>() {
        });

        List<Map<String, String>> keys = (List<Map<String, String>>) jwkMap.get("keys");
        for (Map<String, String> keyData : keys) {
            if (kid.equals(keyData.get("kid"))) {
                RSAPublicKeySpec spec = new RSAPublicKeySpec(
                        new BigInteger(1, Base64.getUrlDecoder().decode(keyData.get("n"))),
                        new BigInteger(1, Base64.getUrlDecoder().decode(keyData.get("e")))
                );
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PublicKey publicKey = kf.generatePublic(spec);

                keyCache.put(kid, publicKey);
                return publicKey;
            }
        }
        throw new IllegalArgumentException("No key found in JWK set for kid: " + kid);
    }
}
