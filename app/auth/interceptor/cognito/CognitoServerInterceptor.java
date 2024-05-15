package app.auth.interceptor.cognito;

import app.auth.user.AuthenticatedUser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import dev.logos.user.AnonymousUser;
import dev.logos.user.User;
import io.grpc.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static dev.logos.user.UserContext.USER_CONTEXT_KEY;
import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;


public class CognitoServerInterceptor implements ServerInterceptor {
    public static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of("Authorization",
                                                                                          ASCII_STRING_MARSHALLER);
    private static final String COGNITO_IDENTITY_POOL_URL_TEMPLATE = "https://cognito-idp.{region}.amazonaws.com/{userPoolId}/.well-known/jwks.json";
    private static final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();
    public static final String BEARER_TYPE = "Bearer";

    private final String userPoolId;
    private final String region;
    private final Logger logger;

    @Inject
    public CognitoServerInterceptor(@Named("CognitoUserPoolId") String userPoolId,
                                    @Named("CognitoRegion") String region,
                                    Logger logger) {
        this.userPoolId = userPoolId;
        this.region = region;
        this.logger = logger;
    }

    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            final Metadata requestHeaders,
            ServerCallHandler<ReqT, RespT> next
    ) {
        Context ctx = Context.current();

        String value = requestHeaders.get(AUTHORIZATION_METADATA_KEY);
        User user = new AnonymousUser();

        if (value != null && value.startsWith(BEARER_TYPE)) {
            user = authenticateUser(value.substring(BEARER_TYPE.length()).trim());
        }

        return Contexts.interceptCall(ctx.withValue(USER_CONTEXT_KEY, user), call, requestHeaders, next);
    }

    private User authenticateUser(String token) {
        try {
            String headerJson = new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]),
                                           StandardCharsets.UTF_8);
            Map<String, String> headerMap = new ObjectMapper().readValue(headerJson, new TypeReference<>() {
            });
            String kid = headerMap.get("kid");

            Jws<Claims> claims = Jwts.parser()
                                     .verifyWith(getPublicKey(kid, userPoolId, region))
                                     .build()
                                     .parseSignedClaims(token);

            claims.getPayload().forEach((key, value) -> logger.log(Level.FINEST, key + ": " + value));

            return new AuthenticatedUser(token, claims.getPayload());
        } catch (ExpiredJwtException | IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.log(Level.SEVERE, "Failed to authenticate");
            return new AnonymousUser();
        }
    }

    private static PublicKey getPublicKey(String kid, String userPoolId, String region) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        PublicKey cachedKey = keyCache.get(kid);
        if (cachedKey != null) {
            return cachedKey;
        }

        String url = COGNITO_IDENTITY_POOL_URL_TEMPLATE.replace("{region}", region).replace("{userPoolId}", userPoolId);
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jwkMap = objectMapper.readValue(new URL(url), new TypeReference<>() {
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
