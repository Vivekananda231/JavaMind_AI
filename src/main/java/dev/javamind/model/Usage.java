package dev.javamind.model;

/**
 * Provider-reported usage. Individual values may be null when a provider omits
 * them; JavaMind never fills the gaps with locally estimated values.
 */
public record Usage(Integer inputTokens, Integer outputTokens, Integer totalTokens) {

    public Usage {
        requireNonNegative(inputTokens, "inputTokens");
        requireNonNegative(outputTokens, "outputTokens");
        requireNonNegative(totalTokens, "totalTokens");
    }

    public static Usage unavailable() {
        return new Usage(null, null, null);
    }

    public boolean isAvailable() {
        return inputTokens != null || outputTokens != null || totalTokens != null;
    }

    private static void requireNonNegative(Integer value, String name) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }
}
