package dev.javamind.exception;

public final class TokenBudgetExceededException extends RuntimeException {

    private final int contextLimit;
    private final int estimatedInputTokens;
    private final int reservedOutputTokens;
    private final int overflowTokens;

    public TokenBudgetExceededException(int contextLimit, int estimatedInputTokens,
                                        int reservedOutputTokens) {
        super("Request exceeds the " + contextLimit + "-token context window: estimated input "
                + estimatedInputTokens + " + reserved output " + reservedOutputTokens
                + " overflows by "
                + (estimatedInputTokens + reservedOutputTokens - contextLimit) + " tokens");
        this.contextLimit = contextLimit;
        this.estimatedInputTokens = estimatedInputTokens;
        this.reservedOutputTokens = reservedOutputTokens;
        this.overflowTokens = estimatedInputTokens + reservedOutputTokens - contextLimit;
    }

    public int contextLimit() {
        return contextLimit;
    }

    public int estimatedInputTokens() {
        return estimatedInputTokens;
    }

    public int reservedOutputTokens() {
        return reservedOutputTokens;
    }

    public int overflowTokens() {
        return overflowTokens;
    }
}
