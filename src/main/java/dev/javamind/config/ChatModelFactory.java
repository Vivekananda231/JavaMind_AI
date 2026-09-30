package dev.javamind.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.javamind.model.ChatModel;
import dev.javamind.provider.langchain4j.LangChain4jChatModelAdapter;
import dev.javamind.provider.raw.RawHttpChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.net.http.HttpClient;
import java.util.Objects;

/** One selection point keeps provider-mode conditionals out of the application. */
public final class ChatModelFactory {

    private final AiConfiguration configuration;

    public ChatModelFactory(AiConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration,
                "configuration must not be null");
    }

    public ChatModel create() {
        return create(configuration.providerType());
    }

    public ChatModel create(AiProviderType providerType) {
        Objects.requireNonNull(providerType, "providerType must not be null");
        return switch (providerType) {
            case RAW -> createRaw();
            case LANGCHAIN4J -> createLangChain4j();
        };
    }

    private ChatModel createRaw() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(configuration.requestTimeout())
                .build();
        return new RawHttpChatModel(httpClient, new ObjectMapper(), configuration.baseUrl(),
                configuration.apiKey(), configuration.requestTimeout());
    }

    private ChatModel createLangChain4j() {
        dev.langchain4j.model.chat.ChatModel langChainModel = OpenAiChatModel.builder()
                .apiKey(configuration.apiKey())
                .baseUrl(configuration.baseUrl().toString())
                .timeout(configuration.requestTimeout())
                .maxRetries(0)
                .build();
        return new LangChain4jChatModelAdapter(langChainModel);
    }
}
