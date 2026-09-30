package dev.javamind.provider.raw.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProviderChatResponse(
        String model,
        List<ProviderChoice> choices,
        ProviderUsage usage) {
}
