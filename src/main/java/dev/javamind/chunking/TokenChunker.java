package dev.javamind.chunking;

import dev.javamind.tokenization.Tokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TokenChunker {

    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("(?:\\R[\\t ]*){2,}");

    private final Tokenizer tokenizer;
    private final int maximumTokensPerChunk;
    private final int overlapTokens;

    public TokenChunker(Tokenizer tokenizer, int maximumTokensPerChunk, int overlapTokens) {
        this.tokenizer = Objects.requireNonNull(tokenizer, "tokenizer must not be null");
        if (maximumTokensPerChunk <= 0) {
            throw new IllegalArgumentException("maximumTokensPerChunk must be positive");
        }
        if (overlapTokens < 0 || overlapTokens >= maximumTokensPerChunk) {
            throw new IllegalArgumentException(
                    "overlapTokens must be between 0 (inclusive) and maximumTokensPerChunk (exclusive)");
        }
        this.maximumTokensPerChunk = maximumTokensPerChunk;
        this.overlapTokens = overlapTokens;
    }

    public List<TokenChunk> chunk(String document) {
        Objects.requireNonNull(document, "document must not be null");
        if (document.isEmpty()) {
            return List.of();
        }

        List<Integer> documentTokens = tokenizer.encode(document);
        if (documentTokens.isEmpty()) {
            return List.of(new TokenChunk(0, document, 0, 0, 0));
        }

        // Convert character paragraph endings into token offsets. String length
        // locates semantic boundaries; it never decides chunk capacity.
        List<Integer> paragraphEnds = paragraphTokenEnds(document);
        List<TokenChunk> chunks = new ArrayList<>();
        int start = 0;

        while (start < documentTokens.size()) {
            int hardEnd = Math.min(start + maximumTokensPerChunk, documentTokens.size());
            int end = bestParagraphEnd(paragraphEnds, start + overlapTokens, hardEnd);
            if (end <= start) {
                end = hardEnd; // oversized paragraph: safe token-level fallback
            }

            List<Integer> chunkTokens = documentTokens.subList(start, end);
            chunks.add(new TokenChunk(chunks.size(), tokenizer.decode(chunkTokens),
                    chunkTokens.size(), start, end));

            if (end == documentTokens.size()) {
                break;
            }
            start = end - Math.min(overlapTokens, end);
        }
        return List.copyOf(chunks);
    }

    private List<Integer> paragraphTokenEnds(String document) {
        List<Integer> ends = new ArrayList<>();
        Matcher matcher = PARAGRAPH_BREAK.matcher(document);
        while (matcher.find()) {
            int tokenOffset = tokenizer.count(document.substring(0, matcher.end()));
            if (ends.isEmpty() || tokenOffset > ends.getLast()) {
                ends.add(tokenOffset);
            }
        }
        int finalOffset = tokenizer.count(document);
        if (ends.isEmpty() || ends.getLast() != finalOffset) {
            ends.add(finalOffset);
        }
        return ends;
    }

    private static int bestParagraphEnd(List<Integer> paragraphEnds, int minimumEndExclusive,
                                        int hardEnd) {
        int best = -1;
        for (int boundary : paragraphEnds) {
            if (boundary > hardEnd) {
                break;
            }
            // The boundary must be beyond the next chunk's overlap. Otherwise
            // subtracting overlap would reproduce the same start forever.
            if (boundary > minimumEndExclusive) {
                best = boundary;
            }
        }
        return best;
    }
}
