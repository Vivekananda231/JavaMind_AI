package dev.javamind.exception;

public final class RateLimitException extends AiClientException {

    public RateLimitException(String message) {
        super(message);
    }
}
