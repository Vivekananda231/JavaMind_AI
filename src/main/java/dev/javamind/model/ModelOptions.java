package dev.javamind.model;

import java.util.Objects;

/** The small set of model controls worth teaching before provider-specific features. */
public record ModelOptions(String model, double temperature, int maxOutputTokens) {

    public ModelOptions {
        Objects.requireNonNull(model, "model must not be null");
        if (model.isBlank()) {
            throw new IllegalArgumentException("model must not be blank");
        }
        if (!Double.isFinite(temperature) || temperature < 0.0) {
            throw new IllegalArgumentException("temperature must be a finite, non-negative number");
        }
        if (maxOutputTokens <= 0) {
            throw new IllegalArgumentException("maxOutputTokens must be positive");
        }
    }
}
