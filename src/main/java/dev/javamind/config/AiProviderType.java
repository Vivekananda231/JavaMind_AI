package dev.javamind.config;

import java.util.Locale;

public enum AiProviderType {
    RAW,
    LANGCHAIN4J;

    public static AiProviderType parse(String value) {
        try {
            return valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "AI_PROVIDER_MODE must be RAW or LANGCHAIN4J", exception);
        }
    }
}
