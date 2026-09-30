package dev.javamind.tokenization;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JTokkitTokenizerTest {

    private final Tokenizer tokenizer = new JTokkitTokenizer();

    @Test
    void encodesAndDecodesWithoutLosingText() {
        String text = "Tokens aren't always words — नमस्ते!";
        List<Integer> tokens = tokenizer.encode(text);

        assertEquals(text, tokenizer.decode(tokens));
        assertEquals(tokens.size(), tokenizer.count(text));
    }

    @Test
    void demonstratesThatWordsAndTokensAreNotEquivalent() {
        assertEquals(1, tokenizer.count("hello"));
        // Punctuation and an uncommon long word make whitespace word-counting inaccurate.
        String text = "hello, antidisestablishmentarianism";
        int whitespaceWords = text.split("\\s+").length;

        org.junit.jupiter.api.Assertions.assertNotEquals(whitespaceWords, tokenizer.count(text));
    }

    @Test
    void resultIsAnImmutableSnapshot() {
        TokenizationResult result = TokenizationResult.from("hello", tokenizer);

        assertThrows(UnsupportedOperationException.class, () -> result.tokens().add(42));
        assertEquals(result.tokens().size(), result.tokenCount());
    }
}
