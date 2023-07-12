package app.auth.service;

import app.auth.proto.auth.AuthServiceGrpc;
import app.auth.proto.auth.Credential;
import app.auth.proto.auth.CredentialsCreateOptions;
import app.auth.proto.auth.CredentialsGetOptions;
import app.auth.proto.auth.FinishAssertionRequest;
import app.auth.proto.auth.FinishAssertionResponse;
import app.auth.proto.auth.StartAssertionRequest;
import app.auth.proto.auth.StartAssertionResponse;
import app.auth.proto.auth.StartRegistrationRequest;
import app.auth.proto.auth.StartRegistrationResponse;
import app.auth.proto.auth.ValidatePublicKeyCredentialRequest;
import app.auth.proto.auth.ValidatePublicKeyCredentialResponse;
import app.auth.storage.AuthCredentialStorage;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Random;
import javax.json.Json;
import javax.json.JsonObject;

enum Cipher {
    RSA("RSA");

    public final String name;

    Cipher(String name) {
        this.name = name;
    }
}

enum SignatureScheme {
    RSA_SHA256("SHA256withRSA");

    public final String name;

    SignatureScheme(String name) {
        this.name = name;
    }
}

public class AuthService extends AuthServiceGrpc.AuthServiceImplBase {

    private static final String RELYING_PARTY_ID = "dev.digits.rip";
    private static final Cipher cipher = Cipher.RSA;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;
    AuthCredentialStorage credentialStorage;

    @Inject
    AuthService(AuthCredentialStorage credentialStorage) {
        this.credentialStorage = credentialStorage;

        String privateKeyPem = System.getenv("SERVER_AUTH_PRIVATE_KEY")
                                     .replace("-----BEGIN PRIVATE KEY-----", "")
                                     .replace("-----END PRIVATE KEY-----", "")
                                     .replaceAll(System.lineSeparator(), "");
        try {
            privateKey = KeyFactory.getInstance(cipher.toString()).generatePrivate(
                new PKCS8EncodedKeySpec(
                    Base64.getDecoder().decode(privateKeyPem)));

            RSAPrivateCrtKey rsaPrivateKey = (RSAPrivateCrtKey) privateKey;
            publicKey = KeyFactory.getInstance(cipher.toString())
                                  .generatePublic(
                                      new RSAPublicKeySpec(rsaPrivateKey.getModulus(),
                                                           rsaPrivateKey.getPublicExponent()));

        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private RelyingParty getRelyingParty() {
        return RelyingParty.builder()
                           .identity(
                               RelyingPartyIdentity.builder()
                                                   .id(RELYING_PARTY_ID)
                                                   .name("Digits")
                                                   .build())
                           .credentialRepository(credentialStorage)
                           .allowOriginPort(true)
                           .build();
    }

    private String makeJwt(String credentialId) {
        return JWT.create()
                  .withIssuer(RELYING_PARTY_ID)
                  .withSubject(credentialId)
                  .withExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                  .sign(Algorithm.RSA256((RSAPublicKey) publicKey,
                                         (RSAPrivateKey) privateKey));
    }

    private String sign(String unencrypted)
        throws SignatureException, NoSuchAlgorithmException, InvalidKeyException {
        Signature privateSignature = Signature.getInstance(SignatureScheme.RSA_SHA256.name);
        privateSignature.initSign(this.privateKey);
        privateSignature.update(unencrypted.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(privateSignature.sign());
    }

    private Boolean verify(String payload,
                           String signature) {
        try {
            Signature sig = Signature.getInstance(SignatureScheme.RSA_SHA256.name);
            sig.initVerify(this.publicKey);
            sig.update(payload.getBytes());
            return sig.verify(Base64.getDecoder().decode(signature));
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            // TODO send all exceptions to an exception logger which can inform metrics and rate-limiting.
            return false;
        }
    }

    @Override
    public void startRegistration(StartRegistrationRequest request,
                                  StreamObserver<StartRegistrationResponse> responseObserver) {
        byte[] userHandle = new byte[64];
        Random random = new Random();
        random.nextBytes(userHandle);

        // TODO check if username is used. display_name can be used by multiple users
        Credential desiredCredential = request.getDesiredCredential();
        if (credentialStorage.usernameExists(desiredCredential.getUsername())) {
            responseObserver.onError(
                Status.ALREADY_EXISTS
                    .withDescription("Username already registered.")
                    .asRuntimeException());

            return;
        }

        PublicKeyCredentialCreationOptions registrationRequest = getRelyingParty().startRegistration(
            StartRegistrationOptions.builder()
                                    .user(UserIdentity.builder()
                                                      .name(desiredCredential.getUsername())
                                                      .displayName(desiredCredential.getDisplayName())
                                                      .id(new ByteArray(userHandle))
                                                      .build())
                                    .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                                                                                          .residentKey(
                                                                                              ResidentKeyRequirement.REQUIRED)
                                                                                          .build())
                                    .build());

        try {
            String credentialCreateJson = registrationRequest.toCredentialsCreateJson();
            responseObserver.onNext(
                StartRegistrationResponse.newBuilder()
                                         .setCredentialsCreateOptions(
                                             CredentialsCreateOptions
                                                 .newBuilder()
                                                 .setPayload(credentialCreateJson)
                                                 .setSignature(sign(credentialCreateJson))
                                                 .build())
                                         .build());
            responseObserver.onCompleted();
        } catch (JsonProcessingException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            responseObserver.onError(
                Status.UNAVAILABLE
                    .withDescription("Registration failure.")
                    .asRuntimeException());
        }
    }

    @Override
    public void validatePublicKeyCredential(
        ValidatePublicKeyCredentialRequest request,
        StreamObserver<ValidatePublicKeyCredentialResponse> responseObserver
    ) {
        CredentialsCreateOptions requestCredentialsCreateOptions = request.getCredentialsCreateOptions();
        String credentialsCreatePayloadStr = requestCredentialsCreateOptions.getPayload();

        if (!verify(credentialsCreatePayloadStr, requestCredentialsCreateOptions.getSignature())) {
            responseObserver.onError(
                Status.PERMISSION_DENIED
                    .withDescription("Invalid signature.")
                    .asRuntimeException());
            return;
        }

        JsonObject publicKey = Json.createReader(new StringReader(credentialsCreatePayloadStr))
                                   .readObject()
                                   .getJsonObject("publicKey");

        try {
            PublicKeyCredentialCreationOptions credentialCreationOptions = PublicKeyCredentialCreationOptions.fromJson(
                publicKey.toString());
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> publicKeyCredential =
                PublicKeyCredential.parseRegistrationResponseJson(request.getPublicKeyCredential());

            RegistrationResult result = getRelyingParty().finishRegistration(
                FinishRegistrationOptions.builder()
                                         .request(PublicKeyCredentialCreationOptions.fromJson(publicKey.toString()))
                                         .response(publicKeyCredential)
                                         .build());

            AuthenticatorAttestationResponse authenticatorAttestationResponse = publicKeyCredential.getResponse();
            PublicKeyCredentialDescriptor resultKeyId = result.getKeyId();

            credentialStorage.create(
                Credential.newBuilder()
                          .setUsername(credentialCreationOptions.getUser().getName())
                          .setDisplayName(credentialCreationOptions.getUser().getDisplayName())
                          .setUserHandle(ByteString.copyFrom(credentialCreationOptions.getUser().getId().getBytes()))
                          .setKeyId(ByteString.copyFrom(resultKeyId.getId().getBytes()))
                          .setPublicKeyCose(ByteString.copyFrom(result.getPublicKeyCose().getBytes()))
                          .setDiscoverable(result.isDiscoverable().orElse(false))
                          .setSignatureCount((int) result.getSignatureCount())
                          .setAttestationObject(ByteString.copyFrom(authenticatorAttestationResponse.getAttestationObject()
                                                                                                    .getBytes()))
                          .setClientDataJson(authenticatorAttestationResponse.getClientDataJSON().toString())
                          .build());

            responseObserver.onNext(
                ValidatePublicKeyCredentialResponse
                    .newBuilder()
                    .setToken(makeJwt(publicKeyCredential.getId().toString()))
                    .build());

            responseObserver.onCompleted();
        } catch (RegistrationFailedException | IOException e) {
            responseObserver.onError(
                Status.UNAVAILABLE
                    .withDescription("Registration failure.")
                    .asRuntimeException());
        }
    }

    @Override
    public void startAssertion(
        StartAssertionRequest request,
        StreamObserver<StartAssertionResponse> responseObserver
    ) {
        AssertionRequest assertionRequest = getRelyingParty().startAssertion(
            StartAssertionOptions.builder().build());

        try {
            String credentialsGetJson = assertionRequest.toJson();
            String credentialsGetSignature = sign(credentialsGetJson);

            responseObserver.onNext(
                StartAssertionResponse.newBuilder()
                                      .setCredentialsGetOptions(
                                          CredentialsGetOptions.newBuilder()
                                                               .setPayload(credentialsGetJson)
                                                               .setSignature(credentialsGetSignature)
                                                               .build())
                                      .build());
            responseObserver.onCompleted();
        } catch (JsonProcessingException | SignatureException | NoSuchAlgorithmException | InvalidKeyException e) {
            responseObserver.onError(
                Status.INVALID_ARGUMENT
                    .withDescription("Assertion failure.")
                    .asRuntimeException());
        }
    }

    @Override
    public void finishAssertion(FinishAssertionRequest request,
                                StreamObserver<FinishAssertionResponse> responseObserver) {
        PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc;
        try {
            pkc = PublicKeyCredential.parseAssertionResponseJson(request.getPublicKeyCredential());
        } catch (IOException e) {
            responseObserver.onError(
                Status.INVALID_ARGUMENT
                    .withDescription("Could not parse public_key_credential as JSON.")
                    .asRuntimeException());
            return;
        }

        CredentialsGetOptions requestCredentialsGetOptions = request.getCredentialsGetOptions();
        String credentialsGetPayloadStr = requestCredentialsGetOptions.getPayload();

        if (!verify(credentialsGetPayloadStr, requestCredentialsGetOptions.getSignature())) {
            responseObserver.onError(
                Status.PERMISSION_DENIED
                    .withDescription("Invalid signature in assertion_request.")
                    .asRuntimeException());
            return;
        }

        AssertionRequest assertionRequest = null;
        try {
            assertionRequest = AssertionRequest.fromJson(credentialsGetPayloadStr);
        } catch (JsonProcessingException e) {
            responseObserver.onError(
                Status.INVALID_ARGUMENT
                    .withDescription("Could not parse assertion_request as JSON.")
                    .asRuntimeException());
            return;
        }

        AssertionResult result = null;
        try {
            result = getRelyingParty().finishAssertion(
                FinishAssertionOptions.builder()
                                      .request(assertionRequest)
                                      .response(pkc)
                                      .build());
        } catch (AssertionFailedException e) {
            responseObserver.onError(
                Status.PERMISSION_DENIED
                    .withDescription("Unsuccessful authentication attempt.")
                    .asRuntimeException());
            return;
        }

        if (result.isSuccess()) {
            responseObserver.onNext(
                FinishAssertionResponse.newBuilder()
                                       .setToken(makeJwt(result.getCredential().getCredentialId().toString()))
                                       .build()
            );
        } else {
            responseObserver.onError(
                Status.PERMISSION_DENIED
                    .withDescription("Unsuccessful authentication attempt.")
                    .asRuntimeException());
            return;
        }
//            responseObserver.onNext(
//            );
        responseObserver.onCompleted();
    }
}

