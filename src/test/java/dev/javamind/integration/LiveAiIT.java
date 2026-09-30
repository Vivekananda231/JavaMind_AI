package dev.javamind.integration;

import dev.javamind.config.AiConfiguration;
import dev.javamind.config.ChatModelFactory;
import dev.javamind.model.ModelOptions;
import dev.javamind.service.ChatExecutionResult;
import dev.javamind.service.ChatService;
import dev.javamind.tokenization.JTokkitTokenizer;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Optional paid/network test, run only by the live-ai Maven profile. */
class LiveAiIT {

    @Test
    void configuredProviderAnswersASmallPrompt() {
        assumeTrue(Stream.of("AI_API_KEY", "AI_BASE_URL", "AI_MODEL")
                .allMatch(name -> System.getenv(name) != null
                        && !System.getenv(name).isBlank()),
                "Live AI environment variables are not configured");

        AiConfiguration configuration = AiConfiguration.fromEnvironment();
        ChatService service = new ChatService(
                new JTokkitTokenizer(),
                new ChatModelFactory(configuration).create(),
                configuration.contextWindowTokens());

        ChatExecutionResult result = service.execute(
                "Answer briefly.",
                "Reply with the word Java.",
                new ModelOptions(configuration.model(), 0.0,
                        Math.min(configuration.maxOutputTokens(), 20)));

        assertFalse(result.response().content().isBlank());
    }
}
