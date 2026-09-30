package dev.javamind.provider.langchain4j;

import dev.javamind.exception.AiClientException;
import dev.javamind.model.ChatMessage;
import dev.javamind.model.ChatModel;
import dev.javamind.model.ChatRequest;
import dev.javamind.model.ChatResponse;
import dev.javamind.model.Usage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.TokenUsage;

import java.util.List;
import java.util.Objects;

/** Adapts LangChain4j 1.20.1 to JavaMind's own ChatModel contract. */
public final class LangChain4jChatModelAdapter implements ChatModel {

    private final dev.langchain4j.model.chat.ChatModel delegate;

    public LangChain4jChatModelAdapter(dev.langchain4j.model.chat.ChatModel delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    public ChatResponse generate(ChatRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        List<dev.langchain4j.data.message.ChatMessage> messages = request.messages().stream()
                .map(this::toLangChain4jMessage)
                .toList();

        dev.langchain4j.model.chat.request.ChatRequest langChainRequest =
                dev.langchain4j.model.chat.request.ChatRequest.builder()
                        .messages(messages)
                        .modelName(request.options().model())
                        .temperature(request.options().temperature())
                        .maxOutputTokens(request.options().maxOutputTokens())
                        .build();

        try {
            dev.langchain4j.model.chat.response.ChatResponse response =
                    delegate.chat(langChainRequest);
            if (response == null || response.aiMessage() == null
                    || response.aiMessage().text() == null
                    || response.aiMessage().text().isBlank()) {
                throw new AiClientException("LangChain4j returned no assistant text");
            }
            return new ChatResponse(
                    response.aiMessage().text(),
                    toUsage(response.tokenUsage()),
                    response.modelName(),
                    response.finishReason() == null ? null : response.finishReason().name());
        } catch (AiClientException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            // The CLI gets a stable, credential-free message while the cause is
            // retained for developers who intentionally inspect a stack trace.
            throw new AiClientException("LangChain4j could not complete the LLM request", exception);
        }
    }

    private dev.langchain4j.data.message.ChatMessage toLangChain4jMessage(ChatMessage message) {
        return switch (message.role()) {
            case SYSTEM -> SystemMessage.from(message.content());
            case USER -> UserMessage.from(message.content());
            case ASSISTANT -> AiMessage.from(message.content());
            // Phase 10 will add real tool-call IDs and names. This neutral
            // placeholder merely keeps today's role model convertible.
            case TOOL -> ToolExecutionResultMessage.from(
                    "javamind-tool-message", "tool", message.content());
        };
    }

    private static Usage toUsage(TokenUsage tokenUsage) {
        return tokenUsage == null
                ? Usage.unavailable()
                : new Usage(tokenUsage.inputTokenCount(), tokenUsage.outputTokenCount(),
                        tokenUsage.totalTokenCount());
    }
}
