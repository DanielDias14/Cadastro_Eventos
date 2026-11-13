package br.com.minispring;

import br.com.minispring.exemplo.Application;
import br.com.minispring.failurefixture.FailureController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(FailureController.class)
class UnexpectedErrorIntegrationTest {
    @LocalServerPort
    private int port;

    @Test
    void unexpectedFailureReturnsJsonWithoutLeakingImplementationDetails() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/failure"))
                    .timeout(Duration.ofSeconds(5)).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(500, response.statusCode());
            var body = new ObjectMapper().readTree(response.body());
            assertEquals("Erro interno do servidor", body.get("message").asText());
            assertFalse(response.body().contains("detalhe-interno-do-teste"));
            assertEquals(response.headers().firstValue("X-Request-Id").orElseThrow(), body.get("requestId").asText());
        }
    }
}
