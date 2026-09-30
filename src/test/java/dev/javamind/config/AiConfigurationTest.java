package dev.javamind.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiConfigurationTest {

    @Test
    void readsRequiredValuesAndSafeDefaults() {
        AiConfiguration configuration = AiConfiguration.fromEnvironment(Map.of(
                "AI_API_KEY", "secret-for-test-only",
                "AI_BASE_URL", "https://example.test/v1",
                "AI_MODEL", "example-model"));

        assertEquals(AiProviderType.RAW, configuration.providerType());
        assertEquals(8192, configuration.contextWindowTokens());
        assertEquals(500, configuration.maxOutputTokens());
        assertEquals(30, configuration.requestTimeout().toSeconds());
        assertFalse(configuration.toString().contains("secret-for-test-only"));
    }

    @Test
    void failsClearlyWhenApiKeyIsMissingWithoutEchoingCredentials() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> AiConfiguration.fromEnvironment(Map.of(
                        "AI_BASE_URL", "https://example.test/v1",
                        "AI_MODEL", "example-model")));

        assertEquals("AI_API_KEY is required for LLM features", exception.getMessage());
    }

    @Test
    void validatesModeAndWindowRelationship() {
        Map<String, String> environment = new HashMap<>(Map.of(
                "AI_API_KEY", "test",
                "AI_BASE_URL", "https://example.test/v1",
                "AI_MODEL", "example-model",
                "AI_PROVIDER_MODE", "not-a-mode"));
        assertThrows(IllegalArgumentException.class,
                () -> AiConfiguration.fromEnvironment(environment));

        environment.put("AI_PROVIDER_MODE", "LANGCHAIN4J");
        environment.put("AI_CONTEXT_WINDOW", "100");
        environment.put("AI_MAX_OUTPUT_TOKENS", "100");
        assertThrows(IllegalArgumentException.class,
                () -> AiConfiguration.fromEnvironment(environment));
    }
}
