package dev.javamind.tokenization;

import java.util.List;
import java.util.Objects;


public record TokenizationResult(String text, List<Integer> tokens) {

    public TokenizationResult {
        Objects.requireNonNull(text, "text must not be null");
        tokens = List.copyOf(Objects.requireNonNull(tokens, "tokens must not be null"));
    }

    public static TokenizationResult from(String text, Tokenizer tokenizer) {
        Objects.requireNonNull(tokenizer, "tokenizer must not be null");
        return new TokenizationResult(text, tokenizer.encode(text));
    }

    public int tokenCount() {
        return tokens.size();
    }
}
