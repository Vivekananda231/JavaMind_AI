package dev.javamind.provider.raw.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProviderChoice(
        ProviderMessage message,
        @JsonProperty("finish_reason") String finishReason) {
}
