package guru.springframework.spring6restmvc.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClientConfigurer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;

public class RealOauthToken {

    public static final JsonNode getNewRealAccessToken(WebTestClient webTestClient){
        // SOLUCIÓN:
        // Llamar al Authorization Server para que nos de un Token válido
        final String AUTHORIZTION_SERVER = "http://localhost:9000/oauth2/token";
        final String BASIC_AUTH_HEADER = "Basic b2lkYy1jbGllbnQ6c2VjcmV0";
        final String GRANT_TYPE = "client_credentials";
        final String BODY = "grant_type: \"client_credentials\"";
        final String SCOPE = "message.read message.write";

        // https://dzone.com/articles/spring-oauth-server-token-claim-customization
        // https://copyprogramming.com/howto/spring-boot-unit-tests-with-jwt-token-security#mocking-jwt-token-in-springboottest-with-webtestclient
        // https://www.baeldung.com/spring-webclient-oauth2
        MultiValueMap<String, String> tokenRequestParams = new LinkedMultiValueMap<>();
        tokenRequestParams.add("grant_type", GRANT_TYPE);
        tokenRequestParams.add("scope", SCOPE);

        JsonNode tokenJwt = webTestClient.post()
                .uri(AUTHORIZTION_SERVER)
                //.header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER)
                .headers(http -> http.setBasicAuth("oidc-client","secret"))
                .accept(MediaType.ALL)
                //.contentType(MediaType.APPLICATION_JSON)
                //.body(BodyInserters.fromFormData("grant_type", "client_credentials"))
                .body(BodyInserters.fromFormData(tokenRequestParams))
                .exchange()
                .expectStatus().isOk()
                .expectBody(JsonNode.class)
//               .jsonPath("$.access_token").exists()
//               .jsonPath("$.token_type").isEqualTo("Bearer")
//               .jsonPath("$.expires_in").isEqualTo(299)
                .returnResult()
                .getResponseBody();

        return tokenJwt;
    }
}
