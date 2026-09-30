package dev.javamind.chunking;

import java.util.Objects;


public record TokenChunk(
        int index,
        String text,
        int tokenCount,
        int startTokenInclusive,
        int endTokenExclusive) {

    public TokenChunk {
        if (index < 0 || tokenCount < 0 || startTokenInclusive < 0
                || endTokenExclusive < startTokenInclusive
                || tokenCount != endTokenExclusive - startTokenInclusive) {
            throw new IllegalArgumentException("Invalid chunk metadata");
        }
        Objects.requireNonNull(text, "text must not be null");
    }
}
