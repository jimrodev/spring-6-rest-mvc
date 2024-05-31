package guru.springframework.spring6restmvc.config;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public class MockOauthToken
{
    // V242
    // MIRAR EL DETALLE DE LA EXPLICACIÓN EN EL * testPatch() *
    // OPCIÓN 1: Genera un Mock del Token JWT, funciona perfectamente con los IT ejecutado con * MockMvc *
    // PROBLEMA
    // Cuando ejecutamos los IT que se ejecutan con WebTestClient, estos fallan porque el siguiente código genera un * MOCK DEL TOKEN JWT * y NOS DA UN 401
    public static final SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtRequestPostProcessor =
            jwt().jwt(jwt -> {
                jwt.claims(claims -> {
                            claims.put("scope", "message-read");
                            claims.put("scope", "message-write");
                        })
                        .subject("messaging-client")
                        .notBefore(Instant.now().minusSeconds(5L));
            });

}
