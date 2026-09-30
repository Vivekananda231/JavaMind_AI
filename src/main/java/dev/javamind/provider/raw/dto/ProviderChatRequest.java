package dev.javamind.provider.raw.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ProviderChatRequest(
        String model,
        List<ProviderMessage> messages,
        double temperature,
        @JsonProperty("max_tokens") int maxTokens) {
}
