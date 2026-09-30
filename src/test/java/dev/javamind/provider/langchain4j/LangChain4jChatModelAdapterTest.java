package dev.javamind.provider.langchain4j;

import dev.javamind.model.ChatMessage;
import dev.javamind.model.ChatRequest;
import dev.javamind.model.ChatResponse;
import dev.javamind.model.ModelOptions;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class LangChain4jChatModelAdapterTest {

    @Test
    void convertsBothDirectionsWithoutANetworkCall() {
        CapturingModel delegate = new CapturingModel();
        LangChain4jChatModelAdapter adapter = new LangChain4jChatModelAdapter(delegate);
        ChatRequest request = new ChatRequest(
                List.of(ChatMessage.system("Teach Java"), ChatMessage.user("Explain records")),
                new ModelOptions("requested-model", 0.4, 80));

        ChatResponse result = adapter.generate(request);

        assertInstanceOf(SystemMessage.class, delegate.request.messages().get(0));
        assertEquals("Teach Java",
                ((SystemMessage) delegate.request.messages().get(0)).text());
        assertInstanceOf(UserMessage.class, delegate.request.messages().get(1));
        assertEquals("Explain records",
                ((UserMessage) delegate.request.messages().get(1)).singleText());
        assertEquals("requested-model", delegate.request.modelName());
        assertEquals(0.4, delegate.request.temperature());
        assertEquals(80, delegate.request.maxOutputTokens());

        assertEquals("Framework answer", result.content());
        assertEquals("actual-model", result.model());
        assertEquals("STOP", result.finishReason());
        assertEquals(12, result.usage().inputTokens());
        assertEquals(5, result.usage().outputTokens());
        assertEquals(17, result.usage().totalTokens());
    }

    private static final class CapturingModel implements dev.langchain4j.model.chat.ChatModel {
        private dev.langchain4j.model.chat.request.ChatRequest request;

        @Override
        public dev.langchain4j.model.chat.response.ChatResponse doChat(
                dev.langchain4j.model.chat.request.ChatRequest request) {
            this.request = request;
            return dev.langchain4j.model.chat.response.ChatResponse.builder()
                    .aiMessage(AiMessage.from("Framework answer"))
                    .modelName("actual-model")
                    .tokenUsage(new TokenUsage(12, 5, 17))
                    .finishReason(FinishReason.STOP)
                    .build();
        }
    }
}
