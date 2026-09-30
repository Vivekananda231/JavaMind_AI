package dev.javamind.context;

import dev.javamind.tokenization.Tokenizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenBudgetTest {

    private final Tokenizer characterTokenizer = new CharacterTokenizer();

    @Test
    void calculatesEveryPartOfTheBudget() {
        TokenBudget budget = TokenBudget.calculate(
                30, "system", "user", List.of("old", "messages"), 5, characterTokenizer);

        assertEquals(6, budget.systemPromptTokens());
        assertEquals(4, budget.userPromptTokens());
        assertEquals(11, budget.conversationTokens());
        assertEquals(5, budget.reservedOutputTokens());
        assertEquals(4, budget.availableContextTokens());
        assertEquals(21, budget.inputTokens());
    }

    @Test
    void rejectsARequestThatCannotFit() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> TokenBudget.calculate(
                        5, "four", "two", List.of(), 1, characterTokenizer));

        assertEquals("Token budget exceeds the context window by 3 tokens", error.getMessage());
    }

    /** Deterministic test double: one UTF-16 character per pretend token. */
    private static final class CharacterTokenizer implements Tokenizer {
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
    }
}
