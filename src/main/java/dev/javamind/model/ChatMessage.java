package dev.javamind.model;

import java.util.Objects;

/** A provider-neutral message; transport-specific message objects stay in adapters. */
public record ChatMessage(MessageRole role, String content) {

    public ChatMessage {
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(content, "content must not be null");
        if (content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
    }

    public static ChatMessage system(String content) {
        return new ChatMessage(MessageRole.SYSTEM, content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(MessageRole.USER, content);
    }
}
