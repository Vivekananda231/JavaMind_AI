package dev.javamind.chunking;

import dev.javamind.tokenization.JTokkitTokenizer;
import dev.javamind.tokenization.Tokenizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenChunkerTest {

    private final Tokenizer tokenizer = new JTokkitTokenizer();

    @Test
    void everyChunkRespectsTheTokenLimitAndReportsItsRealSize() {
        String document = "First paragraph has useful context.\n\n"
                + "Second paragraph is longer and contains several more details.\n\n"
                + "Third paragraph finishes the example.";

        List<TokenChunk> chunks = new TokenChunker(tokenizer, 10, 2).chunk(document);

        assertTrue(chunks.size() > 1);
        for (TokenChunk chunk : chunks) {
            assertTrue(chunk.tokenCount() <= 10);
            assertEquals(chunk.tokenCount(),
                    chunk.endTokenExclusive() - chunk.startTokenInclusive());
            assertEquals(chunk.tokenCount(), tokenizer.encode(chunk.text()).size());
        }
    }

    @Test
    void prefersAParagraphBoundaryWhenItFits() {
        String first = "A short first paragraph.\n\n";
        String document = first + "The next paragraph has more content than the first one.";
        int boundary = tokenizer.count(first);

        List<TokenChunk> chunks = new TokenChunker(tokenizer, boundary + 2, 0).chunk(document);

        assertEquals(boundary, chunks.getFirst().endTokenExclusive());
        assertEquals(first, chunks.getFirst().text());
    }

    @Test
    void fallsBackSafelyForOneOversizedParagraph() {
        String document = "token ".repeat(80);

        List<TokenChunk> chunks = new TokenChunker(tokenizer, 12, 3).chunk(document);

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(chunk -> chunk.tokenCount() <= 12));
        assertEquals(3,
                chunks.getFirst().endTokenExclusive() - chunks.get(1).startTokenInclusive());
    }

    @Test
    void validatesConfigurationAndEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> new TokenChunker(tokenizer, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new TokenChunker(tokenizer, 10, 10));
        assertFalse(new TokenChunker(tokenizer, 10, 0).chunk("").iterator().hasNext());
    }
}
