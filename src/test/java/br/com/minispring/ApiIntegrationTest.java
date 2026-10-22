package br.com.minispring;

import br.com.minispring.framework.MiniApplication;
import br.com.minispring.framework.context.AppConfig;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;

class ApiIntegrationTest {
    private MiniApplication app;
    private HttpClient client;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach void start() throws Exception {
        var properties = new Properties();
        properties.setProperty("app.name", "Centro Cultural de teste");
        app = MiniApplication.start("br.com.minispring.exemplo", new AppConfig(properties, Map.of()), 0);
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }
    @AfterEach void stop() throws Exception {
        if (client != null) client.close();
        if (app != null) app.close();
    }
    private HttpRequest request(String method, String path, String body, String contentType) {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path)).timeout(Duration.ofSeconds(10));
        if (contentType != null) builder.header("Content-Type", contentType);
        return builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build();
    }
    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        return client.send(request(method, path, body, body == null ? null : "application/json"), HttpResponse.BodyHandlers.ofString());
    }
    @Test void fullCrudThroughRealJetty() throws Exception {
        assertEquals("Centro Cultural de teste", json.readTree(send("GET", "/", null).body()).get("application").asText());
        assertEquals(200, send("GET", "/health", null).statusCode());
        var created = send("POST", "/eventos", "{\"titulo\":\" Festival de Música \",\"participantes\":40}");
        assertEquals(201, created.statusCode());
        assertTrue(created.headers().firstValue("Content-Type").orElseThrow().startsWith("application/json"));
        assertFalse(created.headers().firstValue("X-Request-Id").orElseThrow().isBlank());
        String location = created.headers().firstValue("Location").orElseThrow();
        assertEquals("Festival de Música", json.readTree(send("GET", location, null).body()).get("titulo").asText());
        assertEquals(1, json.readTree(send("GET", "/eventos?titulo=MÚS", null).body()).size());
        assertEquals(0, json.readTree(send("GET", "/eventos?titulo=cinema", null).body()).size());
        var updated = send("PUT", location, "{\"titulo\":\"Mostra de Teatro\",\"participantes\":80}");
        assertEquals(200, updated.statusCode());
        assertEquals(80, json.readTree(updated.body()).get("participantes").asInt());
        assertEquals(json.readTree(created.body()).get("id"), json.readTree(updated.body()).get("id"));
        var removed = send("DELETE", location, null);
        assertEquals(204, removed.statusCode());
        assertEquals("", removed.body());
        assertEquals(404, send("GET", location, null).statusCode());
        assertEquals(404, send("DELETE", location, null).statusCode());
        assertEquals(404, send("PUT", location, "{\"titulo\":\"Evento cultural\",\"participantes\":20}").statusCode());
    }
    @Test void rejectsInvalidInputsWithStructuredErrors() throws Exception {
        var invalid = send("POST", "/eventos", "{\"titulo\":\" \",\"participantes\":0}");
        assertEquals(400, invalid.statusCode());
        var error = json.readTree(invalid.body());
        assertEquals(2, error.get("details").size());
        assertEquals(invalid.headers().firstValue("X-Request-Id").orElseThrow(), error.get("requestId").asText());
        for (String body : List.of("{broken", "null", "", "{}", "{\"titulo\":\"Evento cultural\",\"participantes\":1.5}",
                "{\"titulo\":\"Evento cultural\",\"participantes\":\"40\"}", "{} {}", "{\"titulo\":\"Evento cultural\",\"participantes\":40,\"id\":7}")) {
            assertEquals(400, send("POST", "/eventos", body).statusCode(), body);
        }
        assertEquals(400, send("GET", "/eventos/abc", null).statusCode());
        assertEquals(422, send("POST", "/eventos", "{\"titulo\":\"Evento cultural\",\"participantes\":5001}").statusCode());
        assertEquals(415, client.send(request("POST", "/eventos", "{}", "text/plain"), HttpResponse.BodyHandlers.ofString()).statusCode());
        assertEquals(413, send("POST", "/eventos", "x".repeat(1_048_577)).statusCode());
    }
    @Test void distinguishesRouteErrorsAndSendsAllowHeader() throws Exception {
        assertEquals(404, send("GET", "/inexistente", null).statusCode());
        var response = send("PATCH", "/eventos/1", "{}");
        assertEquals(405, response.statusCode());
        assertEquals("DELETE, GET, PUT", response.headers().firstValue("Allow").orElseThrow());
    }
    @Test void concurrentRequestsProduceUniqueIds() throws Exception {
        var futures = new ArrayList<CompletableFuture<HttpResponse<String>>>();
        for (int i = 0; i < 30; i++) futures.add(client.sendAsync(
            request("POST", "/eventos", "{\"titulo\":\"Evento\",\"participantes\":10}", "application/json"), HttpResponse.BodyHandlers.ofString()));
        var ids = new HashSet<Long>();
        for (var future : futures) {
            var response = future.join();
            assertEquals(201, response.statusCode());
            ids.add(json.readTree(response.body()).get("id").asLong());
        }
        assertEquals(30, ids.size());
        assertEquals(30, json.readTree(send("GET", "/eventos", null).body()).size());
    }

    @Test void acceptsInclusiveParticipantLimitsOnCreateAndUpdate() throws Exception {
        var created = send("POST", "/eventos", "{\"titulo\":\"Festival cultural\",\"participantes\":5000}");
        assertEquals(201, created.statusCode());
        String location = created.headers().firstValue("Location").orElseThrow();
        var originalId = json.readTree(created.body()).get("id");
        for (int participants : List.of(1, 5000)) {
            var updated = send("PUT", location,
                "{\"titulo\":\"Mostra cultural\",\"participantes\":" + participants + "}");
            assertEquals(200, updated.statusCode());
            assertEquals(originalId, json.readTree(updated.body()).get("id"));
            assertEquals(participants, json.readTree(send("GET", location, null).body()).get("participantes").asInt());
        }
        assertEquals(201, send("POST", "/eventos", "{\"titulo\":\"Encontro de poesia\",\"participantes\":1}").statusCode());
    }

    @Test void rejectedCreatesAndUpdatesPreserveStoredData() throws Exception {
        var created = send("POST", "/eventos", "{\"titulo\":\"Festival original\",\"participantes\":300}");
        assertEquals(201, created.statusCode());
        String location = created.headers().firstValue("Location").orElseThrow();
        var before = json.readTree(send("GET", "/eventos", null).body());

        var invalidBodies = List.of(
            "{\"titulo\":\"\",\"participantes\":10}",
            "{\"titulo\":\"   \",\"participantes\":10}",
            "{\"titulo\":null,\"participantes\":10}",
            "{\"participantes\":10}",
            "{\"titulo\":\"Alterado\",\"participantes\":0}",
            "{\"titulo\":\"Alterado\",\"participantes\":-1}",
            "{\"titulo\":\"Alterado\",\"participantes\":null}",
            "{\"titulo\":\"Alterado\"}",
            "{\"titulo\":\"Alterado\",\"participantes\":1.5}",
            "{\"titulo\":\"Alterado\",\"participantes\":\"40\"}",
            "{\"titulo\":\"Alterado\",\"participantes\":10,\"id\":99}",
            "{\"titulo\":\"Alterado\",\"participantes\":5001}",
            "null", "", "{invalid"
        );
        for (String body : invalidBodies) {
            int expected = body.contains("5001") ? 422 : 400;
            assertEquals(expected, send("POST", "/eventos", body).statusCode(), body);
            assertEquals(before, json.readTree(send("GET", "/eventos", null).body()), body);
            assertEquals(expected, send("PUT", location, body).statusCode(), body);
            assertEquals(before, json.readTree(send("GET", "/eventos", null).body()), body);
        }
        assertEquals(404, send("PUT", "/eventos/9999", "{\"titulo\":\"Inexistente\",\"participantes\":10}").statusCode());
        assertEquals(404, send("DELETE", "/eventos/9999", null).statusCode());
        assertEquals(before, json.readTree(send("GET", "/eventos", null).body()));
    }

}
