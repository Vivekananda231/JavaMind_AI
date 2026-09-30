package dev.javamind.cli;

import dev.javamind.chunking.TokenChunk;
import dev.javamind.chunking.TokenChunker;
import dev.javamind.config.AiConfiguration;
import dev.javamind.config.AiProviderType;
import dev.javamind.config.ChatModelFactory;
import dev.javamind.context.TokenBudget;
import dev.javamind.model.ChatModel;
import dev.javamind.model.ModelOptions;
import dev.javamind.model.Usage;
import dev.javamind.service.ChatExecutionResult;
import dev.javamind.service.ChatService;
import dev.javamind.tokenization.JTokkitTokenizer;
import dev.javamind.tokenization.TokenizationResult;
import dev.javamind.tokenization.Tokenizer;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public final class PhaseOneDemo {

    private static final String DEFAULT_SYSTEM_INSTRUCTION =
            "You are a Java instructor. Explain concepts clearly to students who already know Core Java.";

    private final Tokenizer tokenizer;
    private final Scanner scanner;

    private PhaseOneDemo(Tokenizer tokenizer, Scanner scanner) {
        this.tokenizer = tokenizer;
        this.scanner = scanner;
    }

    public static void main(String[] args) {
        PhaseOneDemo console = new PhaseOneDemo(new JTokkitTokenizer(), new Scanner(System.in));
        if (args.length > 0) {
            console.tokenize(String.join(" ", args));
            return;
        }
        console.runMenu();
    }

    private void runMenu() {
        boolean running = true;
        while (running) {
            printMenu();
            String choice = prompt("Choose an option: ");
            try {
                switch (choice) {
                    case "1" -> tokenize(prompt("Text: "));
                    case "2" -> decodeTokens();
                    case "3" -> countTokens();
                    case "4" -> calculateBudget();
                    case "5" -> chunkDocument();
                    case "6" -> chatWithModel();
                    case "7" -> compareImplementations();
                    case "0" -> running = false;
                    default -> System.out.println("Unknown option. Choose 0-7.");
                }
            } catch (RuntimeException exception) {

                System.out.println("\nCould not complete the operation: " + exception.getMessage());
            }
            if (running) {
                System.out.println();
            }
        }
        System.out.println("Goodbye from JavaMind.");
    }

    private static void printMenu() {
        System.out.println("""
                        JavaMind Learning Console
                1. Tokenize text
                2. Decode token IDs
                3. Count tokens
                4. Calculate token budget
                5. Chunk document
                6. Chat with LLM
                7. Compare LLM implementations
                0. Exit
                """);
    }

    private void tokenize(String text) {
        TokenizationResult result = TokenizationResult.from(text, tokenizer);
        System.out.println("Token count: " + result.tokenCount());
        System.out.println("Token IDs:   " + result.tokens());
        System.out.println("Decoded:     " + tokenizer.decode(result.tokens()));
    }

    private void decodeTokens() {
        String input = prompt("Comma- or space-separated token IDs: ");
        List<Integer> tokens = Arrays.stream(input.strip().split("[,\\s]+"))
                .filter(value -> !value.isBlank())
                .map(Integer::valueOf)
                .toList();
        System.out.println("Decoded text: " + tokenizer.decode(tokens));
    }

    private void countTokens() {
        String text = prompt("Text: ");
        System.out.println("Token count: " + tokenizer.count(text));
    }

    private void calculateBudget() {
        int contextWindow = readPositiveInt("Context window tokens: ");
        String system = prompt("System prompt: ");
        String user = prompt("User prompt: ");
        int reservedOutput = readPositiveInt("Reserved output tokens: ");
        TokenBudget budget = TokenBudget.calculate(
                contextWindow, system, user, List.of(), reservedOutput, tokenizer);
        System.out.println("System prompt tokens:  " + budget.systemPromptTokens());
        System.out.println("User prompt tokens:    " + budget.userPromptTokens());
        System.out.println("Conversation tokens:   " + budget.conversationTokens());
        System.out.println("Reserved output:       " + budget.reservedOutputTokens());
        System.out.println("Available context:     " + budget.availableContextTokens());
    }

    private void chunkDocument() {
        int maximumTokens = readPositiveInt("Maximum tokens per chunk: ");
        int overlap = readNonNegativeInt("Overlap tokens: ");
        String document = prompt("Document (single line for this CLI demo): ");
        List<TokenChunk> chunks = new TokenChunker(tokenizer, maximumTokens, overlap)
                .chunk(document);
        if (chunks.isEmpty()) {
            System.out.println("The document is empty; no chunks were created.");
            return;
        }
        for (TokenChunk chunk : chunks) {
            System.out.printf("%nChunk %d — %d tokens [%d, %d)%n%s%n",
                    chunk.index() + 1, chunk.tokenCount(), chunk.startTokenInclusive(),
                    chunk.endTokenExclusive(), chunk.text());
        }
    }

    private void chatWithModel() {
        AiConfiguration configuration = AiConfiguration.fromEnvironment();
        ChatModelFactory factory = new ChatModelFactory(configuration);
        String system = promptWithDefault("System instruction", DEFAULT_SYSTEM_INSTRUCTION);
        String user = prompt("User: ");
        ModelOptions options = options(configuration);
        ChatService service = new ChatService(
                tokenizer, factory.create(), configuration.contextWindowTokens());

        System.out.println("\n----------------------------------------");
        System.out.println("REQUEST ANALYSIS");
        System.out.println("----------------------------------------");
        System.out.println("Provider mode:          " + configuration.providerType());
        System.out.println("Model:                  " + configuration.model());
        System.out.println("Reserved output tokens: " + options.maxOutputTokens());

        ChatExecutionResult result = service.execute(system, user, options);
        displayResult(result);
    }

    private void compareImplementations() {
        AiConfiguration configuration = AiConfiguration.fromEnvironment();
        ChatModelFactory factory = new ChatModelFactory(configuration);
        String system = promptWithDefault("System instruction", DEFAULT_SYSTEM_INSTRUCTION);
        String user = prompt("User: ");
        ModelOptions options = options(configuration);

        System.out.println("\nBoth paths send the same JavaMind request to a model. Different wording is");
        System.out.println("normal sampling behavior and does not prove that one integration is better.");
        runComparison("RAW HTTP", factory.create(AiProviderType.RAW), configuration,
                system, user, options);
        runComparison("LANGCHAIN4J", factory.create(AiProviderType.LANGCHAIN4J), configuration,
                system, user, options);
    }

    private void runComparison(String label, ChatModel model, AiConfiguration configuration,
                               String system, String user, ModelOptions options) {
        System.out.println("\n========================================");
        System.out.println(label);
        System.out.println("========================================");
        try {
            ChatService service = new ChatService(
                    tokenizer, model, configuration.contextWindowTokens());
            displayResult(service.execute(system, user, options));
        } catch (RuntimeException exception) {
            System.out.println("Request failed safely: " + exception.getMessage());
        }
    }

    private ModelOptions options(AiConfiguration configuration) {
        String temperatureText = prompt("Temperature [" + configuration.temperature() + "]: ");
        double temperature = temperatureText.isBlank()
                ? configuration.temperature()
                : Double.parseDouble(temperatureText);
        return new ModelOptions(
                configuration.model(), temperature, configuration.maxOutputTokens());
    }

    private static void displayResult(ChatExecutionResult result) {
        System.out.println("Estimated input tokens: " + result.estimatedInputTokens());
        System.out.println("Remaining context:      " + result.remainingContextTokens());
        System.out.println("MODEL RESPONSE");
        System.out.println(result.response().content());
        System.out.println("USAGE");
        Usage usage = result.response().usage();
        System.out.println("Estimated input: " + result.estimatedInputTokens());
        System.out.println("Provider input:   " + display(usage.inputTokens()));
        System.out.println("Provider output:  " + display(usage.outputTokens()));
        System.out.println("Provider total:   " + display(usage.totalTokens()));
        System.out.println("Model:            " + display(result.response().model()));
        System.out.println("Finish reason:    " + display(result.response().finishReason()));
        System.out.println("Request duration: " + result.durationMillis() + " ms");
        System.out.println("\nLocal counts are estimates; provider usage includes its actual message framing.");
    }

    private String prompt(String label) {
        System.out.print(label);
        return scanner.nextLine();
    }

    private String promptWithDefault(String label, String defaultValue) {
        String value = prompt(label + " [" + defaultValue + "]: ");
        return value.isBlank() ? defaultValue : value;
    }

    private int readPositiveInt(String label) {
        int value = Integer.parseInt(prompt(label));
        if (value <= 0) {
            throw new IllegalArgumentException("value must be positive");
        }
        return value;
    }

    private int readNonNegativeInt(String label) {
        int value = Integer.parseInt(prompt(label));
        if (value < 0) {
            throw new IllegalArgumentException("value must not be negative");
        }
        return value;
    }

    private static String display(Object value) {
        return value == null ? "Unavailable" : value.toString();
    }
}
