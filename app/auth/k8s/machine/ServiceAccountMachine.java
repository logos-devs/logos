package app.auth.k8s.machine;

import dev.logos.auth.machine.Machine;
import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.Executor;

public class ServiceAccountMachine extends Machine {
    private static final String TOKEN_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/token";
    private static final String CA_CERT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";

    private static final Optional<PublicKey> PUBLIC_KEY;
    private static final Optional<Exception> PUBLIC_KEY_ERROR;

    static {
        Optional<PublicKey> key = Optional.empty();
        Optional<Exception> error = Optional.empty();
        try {
            byte[] caCertBytes = Files.readAllBytes(Paths.get(CA_CERT_PATH));
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            X509Certificate caCert = (X509Certificate)
                    certFactory.generateCertificate(new ByteArrayInputStream(caCertBytes));
            key = Optional.of(caCert.getPublicKey());
        } catch (IOException | CertificateException e) {
            error = Optional.of(e);
        }
        PUBLIC_KEY = key;
        PUBLIC_KEY_ERROR = error;
    }

    private final String token;
    private final Claims claims;

    // Private constructor to assign pre-validated values.
    private ServiceAccountMachine(String token, Claims claims) {
        this.token = token;
        this.claims = claims;
    }

    public static ServiceAccountMachine self() {
        return forPod().orElseThrow(() -> new IllegalStateException("Unable to find service account token"));
    }

    public static Optional<ServiceAccountMachine> forPod() {
        try {
            String token = readFileContent();
            return fromToken(token);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static Optional<ServiceAccountMachine> fromToken(String token) {
        try {
            Claims claims = validateToken(token);
            return Optional.of(new ServiceAccountMachine(token, claims));
        } catch (IOException | CertificateException | IllegalStateException e) {
            return Optional.empty();
        }
    }

    private static String readFileContent() throws IOException {
        return Files.readString(Paths.get(TOKEN_PATH), StandardCharsets.UTF_8).trim();
    }

    public static Claims validateToken(String token) throws IOException, CertificateException {
        if (getPublicKey().isEmpty()) {
            IllegalStateException ex = new IllegalStateException("Kubernetes CA certificate unavailable, cannot validate token");
            getPublicKeyError().ifPresent(ex::initCause);
            throw ex;
        }

        Jws<Claims> jws = Jwts.parser()
                .verifyWith(getPublicKey().get())
                .build()
                .parseSignedClaims(token);
        Date exp = jws.getPayload().getExpiration();
        if (exp != null && exp.before(new Date())) {
            throw new IllegalStateException("Expired service account token");
        }
        return jws.getPayload();
    }

    static Optional<PublicKey> getPublicKey() {
        return PUBLIC_KEY;
    }

    static Optional<Exception> getPublicKeyError() {
        return PUBLIC_KEY_ERROR;
    }

    @Override
    public String getDisplayName() {
        return claims.get("kubernetes.io/bound-object-ref", String.class);
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public String getId() {
        return claims.get("kubernetes.io/serviceaccount/service-account.uid", String.class);
    }

    @Override
    public String getToken() {
        return token;
    }

    public Claims getClaims() {
        return claims;
    }

    public CallCredentials callCredentials() {
        return new CallCredentials() {
            @Override
            public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {
                Metadata headers = new Metadata();
                headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + token);
                applier.apply(headers);
            }
        };
    }
}
