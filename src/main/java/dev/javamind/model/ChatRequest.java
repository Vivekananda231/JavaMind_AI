package dev.javamind.model;

import java.util.List;
import java.util.Objects;

/**
 * A complete provider-neutral request. The list already supports future history,
 * but Phase 2 deliberately does not store or manage conversation memory.
 */
public record ChatRequest(List<ChatMessage> messages, ModelOptions options) {

    public ChatRequest {
        messages = List.copyOf(Objects.requireNonNull(messages, "messages must not be null"));
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }
        if (messages.stream().noneMatch(message -> message.role() == MessageRole.USER)) {
            throw new IllegalArgumentException("messages must contain at least one USER message");
        }
        Objects.requireNonNull(options, "options must not be null");
    }
}
