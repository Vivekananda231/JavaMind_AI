package dev.javamind.service;

import dev.javamind.context.TokenBudget;
import dev.javamind.exception.TokenBudgetExceededException;
import dev.javamind.model.ChatMessage;
import dev.javamind.model.ChatModel;
import dev.javamind.model.ChatRequest;
import dev.javamind.model.ChatResponse;
import dev.javamind.model.MessageRole;
import dev.javamind.model.ModelOptions;
import dev.javamind.tokenization.Tokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ChatService {

    private final Tokenizer tokenizer;
    private final ChatModel chatModel;
    private final int contextWindowTokens;

    public ChatService(Tokenizer tokenizer, ChatModel chatModel, int contextWindowTokens) {
        this.tokenizer = Objects.requireNonNull(tokenizer, "tokenizer must not be null");
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel must not be null");
        if (contextWindowTokens <= 0) {
            throw new IllegalArgumentException("contextWindowTokens must be positive");
        }
        this.contextWindowTokens = contextWindowTokens;
    }

    public ChatExecutionResult execute(String systemInstruction, String userMessage,
                                       ModelOptions options) {
        ChatRequest request = new ChatRequest(
                List.of(ChatMessage.system(systemInstruction), ChatMessage.user(userMessage)),
                options);
        return execute(request);
    }

    public ChatExecutionResult execute(ChatRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        BudgetInputs inputs = extractBudgetInputs(request.messages());

        TokenBudget budget;
        try {
            budget = TokenBudget.calculate(
                    contextWindowTokens,
                    inputs.systemPrompt(),
                    inputs.userPrompt(),
                    inputs.conversation(),
                    request.options().maxOutputTokens(),
                    tokenizer);
        } catch (IllegalArgumentException exception) {
            int estimatedInputTokens = count(inputs);
            if (estimatedInputTokens + request.options().maxOutputTokens()
                    > contextWindowTokens) {
                throw new TokenBudgetExceededException(
                        contextWindowTokens, estimatedInputTokens,
                        request.options().maxOutputTokens());
            }
            throw exception;
        }

        long startedAt = System.nanoTime();
        ChatResponse response = chatModel.generate(request);
        long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
        return new ChatExecutionResult(
                response, budget.inputTokens(), budget.availableContextTokens(), durationMillis);
    }

    private int count(BudgetInputs inputs) {
        int count = tokenizer.count(inputs.systemPrompt()) + tokenizer.count(inputs.userPrompt());
        for (String message : inputs.conversation()) {
            count = Math.addExact(count, tokenizer.count(message));
        }
        return count;
    }

    private static BudgetInputs extractBudgetInputs(List<ChatMessage> messages) {
        String systemPrompt = messages.stream()
                .filter(message -> message.role() == MessageRole.SYSTEM)
                .map(ChatMessage::content)
                .collect(Collectors.joining("\n"));

        int lastUserIndex = -1;
        for (int index = messages.size() - 1; index >= 0; index--) {
            if (messages.get(index).role() == MessageRole.USER) {
                lastUserIndex = index;
                break;
            }
        }

        String userPrompt = messages.get(lastUserIndex).content();
        List<String> conversation = new ArrayList<>();
        for (int index = 0; index < messages.size(); index++) {
            ChatMessage message = messages.get(index);
            if (message.role() != MessageRole.SYSTEM && index != lastUserIndex) {
                conversation.add(message.content());
            }
        }
        return new BudgetInputs(systemPrompt, userPrompt, List.copyOf(conversation));
    }

    private record BudgetInputs(
            String systemPrompt,
            String userPrompt,
            List<String> conversation) {
    }
}
