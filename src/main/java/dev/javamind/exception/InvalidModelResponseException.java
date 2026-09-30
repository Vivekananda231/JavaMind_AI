package dev.javamind.exception;

public final class InvalidModelResponseException extends AiClientException {

    public InvalidModelResponseException(String message) {
        super(message);
    }

    public InvalidModelResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
