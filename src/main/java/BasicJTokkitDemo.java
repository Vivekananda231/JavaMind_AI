import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;

public class BasicJTokkitDemo {

    public static void main(String[] args) {
        // 1. Create JTokkit's registry of known tokenizer encodings.
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

        // 2. Select cl100k_base, the encoding used by models such as GPT-4.
        Encoding tokenizer = registry.getEncoding(EncodingType.CL100K_BASE);

        String text = "Hello! JavaMind is learning how tokenization works.";

        // 3. Encoding converts text into integer token IDs.
        IntArrayList tokenIds = tokenizer.encode(text);

        System.out.println("Original text: " + text);
        System.out.println("Token IDs:     " + tokenIds);
        System.out.println("Token count:   " + tokenIds.size());

        // 4. Decoding converts the token IDs back into readable text.
        String decodedText = tokenizer.decode(tokenIds);
        System.out.println("Decoded text:  " + decodedText);
    }
}
