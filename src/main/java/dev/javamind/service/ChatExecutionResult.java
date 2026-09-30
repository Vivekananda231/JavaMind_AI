package dev.javamind.service;

import dev.javamind.model.ChatResponse;

import java.util.Objects;

public record ChatExecutionResult(
        ChatResponse response,
        int estimatedInputTokens,
        int remainingContextTokens,
        long durationMillis) {

    public ChatExecutionResult {
        Objects.requireNonNull(response, "response must not be null");
        if (estimatedInputTokens < 0 || remainingContextTokens < 0 || durationMillis < 0) {
            throw new IllegalArgumentException("Execution measurements must not be negative");
        }
    }
}
