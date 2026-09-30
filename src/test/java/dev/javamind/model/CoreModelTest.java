package dev.javamind.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreModelTest {

    @Test
    void validatesChatMessages() {
        assertThrows(NullPointerException.class, () -> new ChatMessage(null, "text"));
        assertThrows(NullPointerException.class, () -> new ChatMessage(MessageRole.USER, null));
        assertThrows(IllegalArgumentException.class,
                () -> new ChatMessage(MessageRole.USER, "  "));
        assertEquals(MessageRole.SYSTEM, ChatMessage.system("Teach Java").role());
    }

    @Test
    void requestIsImmutableAndRequiresAUserMessage() {
        ModelOptions options = new ModelOptions("example-model", 0.2, 100);
        List<ChatMessage> mutable = new ArrayList<>();
        mutable.add(ChatMessage.user("Hello"));
        ChatRequest request = new ChatRequest(mutable, options);
        mutable.clear();

        assertEquals(1, request.messages().size());
        assertThrows(UnsupportedOperationException.class,
                () -> request.messages().add(ChatMessage.user("Again")));
        assertThrows(IllegalArgumentException.class,
                () -> new ChatRequest(List.of(ChatMessage.system("Only system")), options));
    }

    @Test
    void validatesModelOptions() {
        assertThrows(IllegalArgumentException.class, () -> new ModelOptions(" ", 0.2, 100));
        assertThrows(IllegalArgumentException.class,
                () -> new ModelOptions("model", -0.1, 100));
        assertThrows(IllegalArgumentException.class,
                () -> new ModelOptions("model", Double.NaN, 100));
        assertThrows(IllegalArgumentException.class,
                () -> new ModelOptions("model", 0.2, 0));
    }

    @Test
    void usageCanRepresentMissingProviderMetadata() {
        Usage unavailable = Usage.unavailable();
        assertFalse(unavailable.isAvailable());
        assertNull(unavailable.totalTokens());

        Usage usage = new Usage(10, 5, 15);
        assertTrue(usage.isAvailable());
        assertEquals(15, usage.totalTokens());
        assertThrows(IllegalArgumentException.class, () -> new Usage(-1, null, null));
    }

    @Test
    void responseRequiresRealContentAndExplicitUsageState() {
        ChatResponse response = new ChatResponse(
                "Polymorphism selects behavior at runtime.", Usage.unavailable(), null, null);
        assertEquals("Polymorphism selects behavior at runtime.", response.content());
        assertThrows(IllegalArgumentException.class,
                () -> new ChatResponse(" ", Usage.unavailable(), null, null));
        assertThrows(NullPointerException.class,
                () -> new ChatResponse("answer", null, null, null));
    }
}
