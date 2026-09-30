package dev.javamind.provider.raw;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.javamind.exception.AiClientException;
import dev.javamind.exception.AuthenticationException;
import dev.javamind.exception.InvalidModelResponseException;
import dev.javamind.exception.ModelUnavailableException;
import dev.javamind.exception.RateLimitException;
import dev.javamind.model.ChatMessage;
import dev.javamind.model.ChatModel;
import dev.javamind.model.ChatRequest;
import dev.javamind.model.ChatResponse;
import dev.javamind.model.Usage;
import dev.javamind.provider.raw.dto.ProviderChatRequest;
import dev.javamind.provider.raw.dto.ProviderChatResponse;
import dev.javamind.provider.raw.dto.ProviderChoice;
import dev.javamind.provider.raw.dto.ProviderMessage;
import dev.javamind.provider.raw.dto.ProviderUsage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Shows the complete provider lifecycle using the JDK HTTP client and Jackson.
 * No provider SDK or LangChain4j type is used in this implementation.
 */
public final class RawHttpChatModel implements ChatModel {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI chatCompletionsUri;
    private final String apiKey;
    private final Duration requestTimeout;

    public RawHttpChatModel(HttpClient httpClient, ObjectMapper objectMapper, URI baseUrl,
                            String apiKey, Duration requestTimeout) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null")
                .copy()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.chatCompletionsUri = endpoint(Objects.requireNonNull(baseUrl,
                "baseUrl must not be null"));
        this.apiKey = requireText(apiKey, "apiKey");
        this.requestTimeout = Objects.requireNonNull(requestTimeout,
                "requestTimeout must not be null");
    }

    @Override
    public ChatResponse generate(ChatRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        ProviderChatRequest providerRequest = toProviderRequest(request);
        String json = serialize(providerRequest);

        // Provider-neutral data becomes an external HTTP contract only here.
        // Keeping the Authorization value private prevents credentials from
        // appearing in educational output or exception messages.
        HttpRequest httpRequest = HttpRequest.newBuilder(chatCompletionsUri)
                .timeout(requestTimeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofString());
            validateStatus(response.statusCode());
            return parseResponse(response.body());
        } catch (HttpTimeoutException exception) {
            throw new AiClientException("LLM request timed out after "
                    + requestTimeout.toSeconds() + " seconds", exception);
        } catch (IOException exception) {
            throw new AiClientException("Could not communicate with the LLM provider", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiClientException("LLM request was interrupted", exception);
        }
    }

    private ProviderChatRequest toProviderRequest(ChatRequest request) {
        List<ProviderMessage> messages = request.messages().stream()
                .map(this::toProviderMessage)
                .toList();
        return new ProviderChatRequest(
                request.options().model(), messages, request.options().temperature(),
                request.options().maxOutputTokens());
    }

    private ProviderMessage toProviderMessage(ChatMessage message) {
        return new ProviderMessage(message.role().name().toLowerCase(Locale.ROOT), message.content());
    }

    private String serialize(ProviderChatRequest providerRequest) {
        try {
            return objectMapper.writeValueAsString(providerRequest);
        } catch (JsonProcessingException exception) {
            throw new AiClientException("Could not serialize the LLM request", exception);
        }
    }

    private ChatResponse parseResponse(String body) {
        if (body == null || body.isBlank()) {
            throw new InvalidModelResponseException("LLM provider returned an empty response");
        }
        try {
            ProviderChatResponse providerResponse = objectMapper.readValue(
                    body, ProviderChatResponse.class);
            if (providerResponse.choices() == null || providerResponse.choices().isEmpty()) {
                throw new InvalidModelResponseException(
                        "LLM response did not contain a completion choice");
            }
            ProviderChoice choice = providerResponse.choices().getFirst();
            if (choice == null || choice.message() == null
                    || choice.message().content() == null
                    || choice.message().content().isBlank()) {
                throw new InvalidModelResponseException(
                        "LLM response did not contain assistant text");
            }
            return new ChatResponse(choice.message().content(), toUsage(providerResponse.usage()),
                    providerResponse.model(), choice.finishReason());
        } catch (JsonProcessingException exception) {
            throw new InvalidModelResponseException("LLM provider returned invalid JSON", exception);
        }
    }

    private static Usage toUsage(ProviderUsage usage) {
        return usage == null
                ? Usage.unavailable()
                : new Usage(usage.promptTokens(), usage.completionTokens(), usage.totalTokens());
    }

    private static void validateStatus(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }
        switch (statusCode) {
            case 400 -> throw new AiClientException(
                    "LLM provider rejected the request (HTTP 400)");
            case 401, 403 -> throw new AuthenticationException(
                    "LLM provider rejected authentication (HTTP " + statusCode + ")");
            case 404 -> throw new ModelUnavailableException(
                    "LLM endpoint or model was not found (HTTP 404)");
            case 429 -> throw new RateLimitException(
                    "LLM provider rate limit was reached (HTTP 429)");
            default -> {
                if (statusCode >= 500) {
                    throw new ModelUnavailableException(
                            "LLM provider is temporarily unavailable (HTTP " + statusCode + ")");
                }
                throw new AiClientException(
                        "Unexpected LLM provider response (HTTP " + statusCode + ")");
            }
        }
    }

    private static URI endpoint(URI baseUrl) {
        String value = baseUrl.toString();
        URI normalized = URI.create(value.endsWith("/") ? value : value + "/");
        return normalized.resolve("chat/completions");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
