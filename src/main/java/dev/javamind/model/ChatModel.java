package dev.javamind.model;

/**
 * Dependency-inversion boundary for model communication. Application code uses
 * this interface, so switching HTTP libraries or AI frameworks cannot leak into
 * token budgeting, CLI, or future business logic.
 */
@FunctionalInterface
public interface ChatModel {

    ChatResponse generate(ChatRequest request);
}
