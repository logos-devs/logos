package app.auth.cognito.interceptor.cognito;

import app.auth.cognito.module.data.CognitoStackOutputs;
import app.auth.cognito.user.AuthenticatedUser;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import dev.logos.stack.aws.module.annotation.AwsRegion;
import dev.logos.user.AnonymousUser;
import dev.logos.user.User;
import io.grpc.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static dev.logos.user.UserContext.USER_CONTEXT_KEY;
import static java.lang.System.err;
import static java.util.Objects.requireNonNull;

public class CognitoServerInterceptor implements ServerInterceptor {

    public static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> COOKIE_METADATA_KEY = Metadata.Key.of("logos-cookies", Metadata.ASCII_STRING_MARSHALLER);
    private static final String COGNITO_IDENTITY_POOL_URL_TEMPLATE = "https://cognito-idp.{region}.amazonaws.com/{userPoolId}/.well-known/jwks.json";

    private final Cache<String, PublicKey> keyCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(100)
            .build();

    private final String userPoolId;
    private final String region;
    private final Logger logger;

    @Inject
    public CognitoServerInterceptor(final CognitoStackOutputs cognitoStackOutputs, @AwsRegion String region, Logger logger) {
        this.userPoolId = requireNonNull(cognitoStackOutputs.cognitoUserPoolId());
        this.region = requireNonNull(region);
        this.logger = logger;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, final Metadata requestHeaders, ServerCallHandler<ReqT, RespT> next) {
        Context ctx = Context.current();
        User user = new AnonymousUser();
        Metadata internalRequestHeaders = new Metadata();
        internalRequestHeaders.merge(requestHeaders);

        Optional<String> idToken = Optional.ofNullable(requestHeaders.get(AUTHORIZATION_METADATA_KEY))
                .filter(authHeader -> authHeader.startsWith("Bearer "))
                .map(authHeader -> authHeader.substring("Bearer ".length()))
                .or(() -> Optional.ofNullable(requestHeaders.get(COOKIE_METADATA_KEY))
                        .flatMap(this::extractIdTokenFromCookies));

        if (idToken.isPresent()) {
            String token = idToken.get();
            user = authenticateUser(token);
            internalRequestHeaders.put(AUTHORIZATION_METADATA_KEY, "Bearer " + token);
        }

        return Contexts.interceptCall(ctx.withValue(USER_CONTEXT_KEY, user), call, internalRequestHeaders, next);
    }

    private Optional<String> extractIdTokenFromCookies(String cookieHeader) {
        String[] cookies = cookieHeader.split(";");

        for (String cookie : cookies) {
            String trimmedCookie = cookie.trim();
            if (trimmedCookie.startsWith("logosIdToken=")) {
                return Optional.of(trimmedCookie.substring("logosIdToken=".length()));
            }
        }
        return Optional.empty();
    }

    private User authenticateUser(String token) {
        try {
            String headerJson = new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            Map<String, String> headerMap = gson.fromJson(headerJson, new TypeToken<Map<String, String>>() {
            }.getType());
            String kid = headerMap.get("kid");

            Jws<Claims> claims = Jwts.parser()
                    .verifyWith(getPublicKey(kid, userPoolId, region))
                    .build()
                    .parseSignedClaims(token);

            return new AuthenticatedUser(token, claims.getPayload());
        } catch (ExpiredJwtException | IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.log(Level.SEVERE, "Failed to authenticate", e);
            return new AnonymousUser();
        }
    }

    private PublicKey getPublicKey(String kid, String userPoolId, String region)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        PublicKey cachedKey = keyCache.getIfPresent(kid);
        if (cachedKey != null) {
            return cachedKey;
        }

        String url = COGNITO_IDENTITY_POOL_URL_TEMPLATE.replace("{region}", region).replace("{userPoolId}", userPoolId);
        Gson gson = new Gson();

        try (InputStreamReader reader = new InputStreamReader(new URL(url).openStream())) {
            JsonObject jwkResponse = gson.fromJson(reader, JsonObject.class);
            JsonArray keys = jwkResponse.getAsJsonArray("keys");

            for (JsonElement keyElement : keys) {
                JsonObject keyData = keyElement.getAsJsonObject();
                if (kid.equals(keyData.get("kid").getAsString())) {
                    RSAPublicKeySpec spec = new RSAPublicKeySpec(
                            new BigInteger(1, Base64.getUrlDecoder().decode(keyData.get("n").getAsString())),
                            new BigInteger(1, Base64.getUrlDecoder().decode(keyData.get("e").getAsString()))
                    );
                    KeyFactory kf = KeyFactory.getInstance("RSA");
                    PublicKey publicKey = kf.generatePublic(spec);

                    // Cache the key with a time-based expiration
                    keyCache.put(kid, publicKey);
                    return publicKey;
                }
            }
        } catch (JsonSyntaxException e) {
            throw new IOException("Error parsing the JWK response", e);
        } catch (IllegalArgumentException e) {
            throw new InvalidKeySpecException("Invalid key specification", e);
        }

        throw new IllegalArgumentException("No key found in JWK set for kid: " + kid);
    }
}
