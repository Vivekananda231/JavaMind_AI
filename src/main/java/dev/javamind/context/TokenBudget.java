package dev.javamind.context;

import dev.javamind.tokenization.Tokenizer;

import java.util.List;
import java.util.Objects;

public record TokenBudget(
        int systemPromptTokens,
        int userPromptTokens,
        int conversationTokens,
        int reservedOutputTokens,
        int availableContextTokens) {

    public TokenBudget {
        requireNonNegative(systemPromptTokens, "systemPromptTokens");
        requireNonNegative(userPromptTokens, "userPromptTokens");
        requireNonNegative(conversationTokens, "conversationTokens");
        requireNonNegative(reservedOutputTokens, "reservedOutputTokens");
        requireNonNegative(availableContextTokens, "availableContextTokens");
    }

    public static TokenBudget calculate(
            int contextWindowTokens,
            String systemPrompt,
            String userPrompt,
            List<String> conversation,
            int reservedOutputTokens,
            Tokenizer tokenizer) {

        if (contextWindowTokens <= 0) {
            throw new IllegalArgumentException("contextWindowTokens must be positive");
        }
        requireNonNegative(reservedOutputTokens, "reservedOutputTokens");
        Objects.requireNonNull(systemPrompt, "systemPrompt must not be null");
        Objects.requireNonNull(userPrompt, "userPrompt must not be null");
        Objects.requireNonNull(conversation, "conversation must not be null");
        Objects.requireNonNull(tokenizer, "tokenizer must not be null");

        int systemTokens = tokenizer.count(systemPrompt);
        int userTokens = tokenizer.count(userPrompt);
        int historyTokens = conversation.stream()
                .mapToInt(message -> tokenizer.count(
                        Objects.requireNonNull(message, "conversation message must not be null")))
                .sum();

        int used = Math.addExact(Math.addExact(systemTokens, userTokens), historyTokens);
        used = Math.addExact(used, reservedOutputTokens);
        int available = contextWindowTokens - used;
        if (available < 0) {
            throw new IllegalArgumentException(
                    "Token budget exceeds the context window by " + -available + " tokens");
        }

        return new TokenBudget(systemTokens, userTokens, historyTokens,
                reservedOutputTokens, available);
    }

    public int inputTokens() {
        return systemPromptTokens + userPromptTokens + conversationTokens;
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }
}
