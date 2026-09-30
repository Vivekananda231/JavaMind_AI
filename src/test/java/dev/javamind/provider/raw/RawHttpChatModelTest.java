package dev.javamind.provider.raw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.javamind.exception.AuthenticationException;
import dev.javamind.exception.InvalidModelResponseException;
import dev.javamind.exception.ModelUnavailableException;
import dev.javamind.exception.RateLimitException;
import dev.javamind.model.ChatMessage;
import dev.javamind.model.ChatRequest;
import dev.javamind.model.ChatResponse;
import dev.javamind.model.ModelOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RawHttpChatModelTest {

    private static final String TEST_KEY = "unit-test-secret-never-a-real-key";

    private final AtomicReference<ResponseSpec> response = new AtomicReference<>();
    private final AtomicReference<String> capturedBody = new AtomicReference<>();
    private final AtomicReference<String> capturedContentType = new AtomicReference<>();
    private HttpServer server;
    private ObjectMapper objectMapper;
    private RawHttpChatModel model;

    @BeforeEach
    void startServer() throws IOException {
        objectMapper = new ObjectMapper();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v1/chat/completions", this::handle);
        server.start();
        URI baseUrl = URI.create("http://localhost:" + server.getAddress().getPort() + "/v1");
        model = new RawHttpChatModel(
                HttpClient.newHttpClient(), objectMapper, baseUrl, TEST_KEY,
                Duration.ofSeconds(2));
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void convertsRequestToJsonAndResponseBackToCoreModel() throws Exception {
        response.set(new ResponseSpec(200, """
                {
                  "model": "provider-model",
                  "choices": [{
                    "message": {"role": "assistant", "content": "A clear answer."},
                    "finish_reason": "stop"
                  }],
                  "usage": {"prompt_tokens": 14, "completion_tokens": 4, "total_tokens": 18}
                }
                """));

        ChatResponse result = model.generate(request());

        assertEquals("A clear answer.", result.content());
        assertEquals("provider-model", result.model());
        assertEquals("stop", result.finishReason());
        assertEquals(14, result.usage().inputTokens());
        assertEquals(4, result.usage().outputTokens());
        assertEquals(18, result.usage().totalTokens());

        JsonNode json = objectMapper.readTree(capturedBody.get());
        assertEquals("requested-model", json.get("model").asText());
        assertEquals(0.3, json.get("temperature").asDouble());
        assertEquals(50, json.get("max_tokens").asInt());
        assertEquals("system", json.get("messages").get(0).get("role").asText());
        assertEquals("Teach Java", json.get("messages").get(0).get("content").asText());
        assertEquals("user", json.get("messages").get(1).get("role").asText());
        assertEquals("application/json", capturedContentType.get());
    }

    @ParameterizedTest
    @MethodSource("errorStatuses")
    void mapsImportantHttpErrors(int status, Class<? extends RuntimeException> expectedType) {
        response.set(new ResponseSpec(status, "{\"error\":\"details are not echoed\"}"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> model.generate(request()));

        assertInstanceOf(expectedType, exception);
        assertTrue(exception.getMessage().contains(Integer.toString(status)));
        assertFalse(exception.getMessage().contains(TEST_KEY));
    }

    @Test
    void rejectsInvalidJson() {
        response.set(new ResponseSpec(200, "not-json"));

        assertThrows(InvalidModelResponseException.class, () -> model.generate(request()));
    }

    @Test
    void rejectsEmptyResponse() {
        response.set(new ResponseSpec(200, ""));

        assertThrows(InvalidModelResponseException.class, () -> model.generate(request()));
    }

    private static Stream<Arguments> errorStatuses() {
        return Stream.of(
                Arguments.of(401, AuthenticationException.class),
                Arguments.of(429, RateLimitException.class),
                Arguments.of(500, ModelUnavailableException.class));
    }

    private ChatRequest request() {
        return new ChatRequest(
                List.of(ChatMessage.system("Teach Java"), ChatMessage.user("Explain interfaces")),
                new ModelOptions("requested-model", 0.3, 50));
    }

    private void handle(HttpExchange exchange) throws IOException {
        capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        capturedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
        ResponseSpec spec = response.get();
        byte[] bytes = spec.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(spec.status(), bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private record ResponseSpec(int status, String body) {
    }
}
