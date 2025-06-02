package app.auth.k8s.machine;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Date;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ServiceAccountMachineTest {

    private static void setStaticFinalField(Class<?> cls, String name, Object value) throws Exception {
        Field field = cls.getDeclaredField(name);
        field.setAccessible(true);
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, value);
    }

    @Test
    public void fromToken_rejectsExpiredToken() {
        KeyPair kp = Jwts.SIG.RS256.keyPair().build();

        // mock the key and error providers
        try (MockedStatic<ServiceAccountMachine> svc = mockStatic(ServiceAccountMachine.class)) {
            svc.when(ServiceAccountMachine::getPublicKey)
                    .thenReturn(Optional.of(kp.getPublic()));
            svc.when(ServiceAccountMachine::getPublicKeyError)
                    .thenReturn(Optional.empty());

            // existing stubbing of JWT parsing...
            Claims claims = mock(Claims.class);
            when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() - 1_000));

            Jws<Claims> jws = mock(Jws.class);
            when(jws.getPayload()).thenReturn(claims);

            JwtParser parser = mock(JwtParser.class);
            when(parser.parseSignedClaims(anyString())).thenReturn(jws);

            JwtParserBuilder builder = mock(JwtParserBuilder.class);
            when(builder.verifyWith(any(PublicKey.class))).thenReturn(builder);
            when(builder.build()).thenReturn(parser);

            try (MockedStatic<Jwts> jwts = mockStatic(Jwts.class)) {
                jwts.when(Jwts::parser).thenReturn(builder);

                Optional<ServiceAccountMachine> machine = ServiceAccountMachine.fromToken("dummy");
                assertTrue(machine.isEmpty());
            }
        }
    }
}
