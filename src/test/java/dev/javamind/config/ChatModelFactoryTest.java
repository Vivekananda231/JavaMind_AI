package dev.javamind.config;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.javamind.model.ChatMessage;
import dev.javamind.model.ChatModel;
import dev.javamind.model.ChatRequest;
import dev.javamind.model.ChatResponse;
import dev.javamind.model.ModelOptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatModelFactoryTest {

    @Test
    void langChain4jModeWorksAgainstAnOfflineOpenAiCompatibleServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v1/chat/completions", this::respond);
        server.start();
        try {
            AiConfiguration configuration = new AiConfiguration(
                    "offline-test-key",
                    URI.create("http://localhost:" + server.getAddress().getPort() + "/v1"),
                    "test-model",
                    AiProviderType.LANGCHAIN4J,
                    1000,
                    50,
                    0.2,
                    Duration.ofSeconds(2));
            ChatModel model = new ChatModelFactory(configuration).create();

            ChatResponse response = model.generate(new ChatRequest(
                    List.of(ChatMessage.user("Say hello")),
                    new ModelOptions("test-model", 0.2, 50)));

            assertEquals("Hello from the local server.", response.content());
            assertEquals(3, response.usage().inputTokens());
            assertEquals(6, response.usage().totalTokens());
        } finally {
            server.stop(0);
        }
    }

    private void respond(HttpExchange exchange) throws IOException {
        exchange.getRequestBody().readAllBytes();
        byte[] body = """
                {
                  "id": "chatcmpl-offline",
                  "object": "chat.completion",
                  "created": 1,
                  "model": "test-model",
                  "choices": [{
                    "index": 0,
                    "message": {"role": "assistant", "content": "Hello from the local server."},
                    "finish_reason": "stop"
                  }],
                  "usage": {"prompt_tokens": 3, "completion_tokens": 3, "total_tokens": 6}
                }
                """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
