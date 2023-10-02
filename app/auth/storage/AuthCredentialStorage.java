package app.auth.storage;

import app.auth.proto.auth.Credential;
import com.querydsl.core.Tuple;
import com.querydsl.sql.SQLQuery;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import dev.logos.stack.service.storage.TableStorage;
import dev.logos.stack.service.storage.pg.meta.Auth;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static dev.logos.stack.service.storage.pg.meta.auth.QCredential.credential;
import static java.util.Objects.requireNonNull;

public class AuthCredentialStorage extends TableStorage<Credential, UUID> implements CredentialRepository {

    public AuthCredentialStorage() {
        super(Auth.credential, Credential.class, UUID.class);
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return getQueryFactory().select(credential.keyId)
                                .from(credential)
                                .where(credential.username.eq(username))
                                .stream()
                                .map(bytes -> PublicKeyCredentialDescriptor.builder()
                                                                           .id(new ByteArray(bytes))
                                                                           .build())
                                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        byte[] result = getQueryFactory().select(credential.userHandle)
                                         .from(credential)
                                         .where(credential.username.eq(username))
                                         .fetchFirst();
        return result == null ? Optional.empty() : Optional.of(new ByteArray(result));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return Optional.ofNullable(
            getQueryFactory().select(credential.username)
                             .from(credential)
                             .where(credential.userHandle.eq(userHandle.getBytes()))
                             .fetchOne());
    }

    private SQLQuery<Tuple> lookupQuery() {
        return getQueryFactory()
            .select(credential.keyId,
                    credential.id,
                    credential.userHandle,
                    credential.publicKeyCose,
                    credential.signatureCount)
            .from(credential);
    }

    private RegisteredCredential registeredCredentialMapper(Tuple tuple) {
        return RegisteredCredential.builder()
                                   .credentialId(new ByteArray(requireNonNull(tuple.get(credential.keyId))))
                                   .userHandle(new ByteArray(requireNonNull(tuple.get(credential.userHandle))))
                                   .publicKeyCose(new ByteArray(requireNonNull(tuple.get(credential.publicKeyCose))))
                                   .signatureCount(tuple.get(credential.signatureCount))
                                   .build();
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId,
                                                 ByteArray userHandle) {
        return lookupQuery().where(credential.keyId.eq(credentialId.getBytes())
                                                   .and(credential.userHandle.eq(userHandle.getBytes())))
                            .stream()
                            .map(this::registeredCredentialMapper)
                            .findFirst();
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return lookupQuery().where(credential.keyId.eq(credentialId.getBytes()))
                            .stream()
                            .map(this::registeredCredentialMapper)
                            .collect(Collectors.toUnmodifiableSet());
    }

    // FIXME figure out how to express an exists() query in querydsl
    public boolean usernameExists(String username) {
        return getQueryFactory().from(credential)
                                .where(credential.username.eq(username))
                                .fetchCount() > 0;
    }
}
