package dev.javamind.config;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public record AiConfiguration(
        String apiKey,
        URI baseUrl,
        String model,
        AiProviderType providerType,
        int contextWindowTokens,
        int maxOutputTokens,
        double temperature,
        Duration requestTimeout) {

    public AiConfiguration {
        requireText(apiKey, "AI_API_KEY");
        Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        requireText(model, "AI_MODEL");
        Objects.requireNonNull(providerType, "providerType must not be null");
        if (contextWindowTokens <= 0) {
            throw new IllegalArgumentException("AI_CONTEXT_WINDOW must be positive");
        }
        if (maxOutputTokens <= 0) {
            throw new IllegalArgumentException("AI_MAX_OUTPUT_TOKENS must be positive");
        }
        if (maxOutputTokens >= contextWindowTokens) {
            throw new IllegalArgumentException(
                    "AI_MAX_OUTPUT_TOKENS must be smaller than AI_CONTEXT_WINDOW");
        }
        if (!Double.isFinite(temperature) || temperature < 0) {
            throw new IllegalArgumentException("AI_TEMPERATURE must be finite and non-negative");
        }
        Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");
        if (requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("AI_REQUEST_TIMEOUT_SECONDS must be positive");
        }
    }

    public static AiConfiguration fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    /** Prevent accidental credential exposure when configuration is inspected. */
    @Override
    public String toString() {
        return "AiConfiguration[apiKey=<redacted>, baseUrl=" + baseUrl
                + ", model=" + model
                + ", providerType=" + providerType
                + ", contextWindowTokens=" + contextWindowTokens
                + ", maxOutputTokens=" + maxOutputTokens
                + ", temperature=" + temperature
                + ", requestTimeout=" + requestTimeout + "]";
    }

    static AiConfiguration fromEnvironment(Map<String, String> environment) {
        Objects.requireNonNull(environment, "environment must not be null");
        return new AiConfiguration(
                required(environment, "AI_API_KEY"),
                parseUri(required(environment, "AI_BASE_URL")),
                required(environment, "AI_MODEL"),
                AiProviderType.parse(environment.getOrDefault("AI_PROVIDER_MODE", "RAW")),
                parseInt(environment, "AI_CONTEXT_WINDOW", 8192),
                parseInt(environment, "AI_MAX_OUTPUT_TOKENS", 500),
                parseDouble(environment, "AI_TEMPERATURE", 0.2),
                Duration.ofSeconds(parseInt(environment, "AI_REQUEST_TIMEOUT_SECONDS", 30)));
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required for LLM features");
        }
        return value.strip();
    }

    private static URI parseUri(String value) {
        try {
            URI uri = URI.create(value);
            if (!uri.isAbsolute() || uri.getHost() == null) {
                throw new IllegalArgumentException("AI_BASE_URL must be an absolute HTTP(S) URL");
            }
            if (!"http".equalsIgnoreCase(uri.getScheme())
                    && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException("AI_BASE_URL must use HTTP or HTTPS");
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("AI_BASE_URL is invalid", exception);
        }
    }

    private static int parseInt(Map<String, String> environment, String name, int defaultValue) {
        try {
            return Integer.parseInt(environment.getOrDefault(name, Integer.toString(defaultValue)));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be an integer", exception);
        }
    }

    private static double parseDouble(Map<String, String> environment, String name,
                                      double defaultValue) {
        try {
            return Double.parseDouble(environment.getOrDefault(name, Double.toString(defaultValue)));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a number", exception);
        }
    }

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
