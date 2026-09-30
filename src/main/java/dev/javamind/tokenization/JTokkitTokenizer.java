package dev.javamind.tokenization;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;

import java.util.List;
import java.util.Objects;

/**
 * JTokkit adapter for {@code cl100k_base}, the encoding used by GPT-4 and the
 * text-embedding-3 models. No other JavaMind class imports JTokkit types.
 */
public final class JTokkitTokenizer implements Tokenizer {

    private final Encoding encoding;

    public JTokkitTokenizer() {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        this.encoding = registry.getEncoding(EncodingType.CL100K_BASE);
    }

    @Override
    public List<Integer> encode(String text) {
        Objects.requireNonNull(text, "text must not be null");
        return List.copyOf(encoding.encode(text).boxed());
    }

    @Override
    public String decode(List<Integer> tokens) {
        Objects.requireNonNull(tokens, "tokens must not be null");
        IntArrayList tokenIds = new IntArrayList(tokens.size());
        tokens.forEach(token -> tokenIds.add(
                Objects.requireNonNull(token, "token ID must not be null")));
        return encoding.decode(tokenIds);
    }

    @Override
    public int count(String text) {
        Objects.requireNonNull(text, "text must not be null");
        return encoding.countTokens(text);
    }
}
