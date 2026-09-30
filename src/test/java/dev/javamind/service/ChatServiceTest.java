package dev.javamind.service;

import dev.javamind.exception.TokenBudgetExceededException;
import dev.javamind.model.ChatModel;
import dev.javamind.model.ChatRequest;
import dev.javamind.model.ChatResponse;
import dev.javamind.model.ModelOptions;
import dev.javamind.model.Usage;
import dev.javamind.tokenization.Tokenizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatServiceTest {

    @Test
    void smallPromptUsesPhaseOneTokenizerAndCallsModel() {
        CountingTokenizer tokenizer = new CountingTokenizer();
        RecordingChatModel model = new RecordingChatModel();
        ChatService service = new ChatService(tokenizer, model, 100);

        ChatExecutionResult result = service.execute(
                "system", "user", new ModelOptions("test-model", 0.1, 20));

        assertTrue(tokenizer.countCalls > 0);
        assertTrue(model.called);
        assertEquals(10, result.estimatedInputTokens());
        assertEquals(70, result.remainingContextTokens());
        assertEquals("fake answer", result.response().content());
    }

    @Test
    void oversizedPromptIsRejectedBeforeModelInvocation() {
        CountingTokenizer tokenizer = new CountingTokenizer();
        RecordingChatModel model = new RecordingChatModel();
        ChatService service = new ChatService(tokenizer, model, 12);

        TokenBudgetExceededException exception = assertThrows(
                TokenBudgetExceededException.class,
                () -> service.execute(
                        "system", "oversized", new ModelOptions("test-model", 0.1, 4)));

        assertFalse(model.called);
        assertEquals(12, exception.contextLimit());
        assertEquals(15, exception.estimatedInputTokens());
        assertEquals(4, exception.reservedOutputTokens());
        assertEquals(7, exception.overflowTokens());
    }

    private static final class CountingTokenizer implements Tokenizer {
        private int countCalls;

        @Override
        public List<Integer> encode(String text) {
            return text.chars().boxed().toList();
        }

        @Override
        public String decode(List<Integer> tokens) {
            StringBuilder result = new StringBuilder();
            tokens.forEach(result::appendCodePoint);
            return result.toString();
        }

        @Override
        public int count(String text) {
            countCalls++;
            return text.length();
        }
    }

    private static final class RecordingChatModel implements ChatModel {
        private boolean called;

        @Override
        public ChatResponse generate(ChatRequest request) {
            called = true;
            return new ChatResponse("fake answer", new Usage(10, 2, 12),
                    request.options().model(), "stop");
        }
    }
}
