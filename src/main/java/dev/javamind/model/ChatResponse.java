package dev.javamind.model;

import java.util.Objects;

public record ChatResponse(String content, Usage usage, String model, String finishReason) {

    public ChatResponse {
        Objects.requireNonNull(content, "content must not be null");
        if (content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        Objects.requireNonNull(usage, "usage must not be null; use Usage.unavailable()");
    }
}
