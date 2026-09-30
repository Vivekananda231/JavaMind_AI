# Raw HTTP vs LangChain4j

JavaMind intentionally implements the same `ChatModel` capability twice. The
same `ChatService`, tokenizer, budget, request, response, configuration, and CLI
sit above both implementations.

| Concern | Raw HTTP | LangChain4j adapter |
|---|---|---|
| Amount of integration code | More; JavaMind owns the transport path | Less; framework owns common integration work |
| HTTP handling | Explicit JDK `HttpClient`, URI, headers, POST, timeout | Configured through `OpenAiChatModel` |
| JSON handling | Explicit Jackson serialization/deserialization | Internal to LangChain4j integration |
| Message conversion | JavaMind messages to provider DTOs | JavaMind messages to LangChain4j messages |
| Error handling | JavaMind maps HTTP statuses and malformed responses | Framework raises integration exceptions; adapter exposes a safe JavaMind exception |
| Provider abstraction | JavaMind owns this particular OpenAI-compatible contract | LangChain4j supplies Java-native model integrations |
| Maintainability | More code and full control; API changes are ours to handle | Less boilerplate; framework version/API changes must be managed |
| Educational value | Makes every network and transport boundary visible | Shows why mature abstractions are valuable after learning the boundary |

## Raw HTTP path

`RawHttpChatModel` uses only normal Java networking and Jackson:

```text
ChatRequest
    ↓
ProviderChatRequest / ProviderMessage
    ↓
Jackson JSON
    ↓
java.net.http.HttpRequest
    ↓
HTTP response
    ↓
ProviderChatResponse
    ↓
ChatResponse
```

### Strengths

- Maximum visibility and control over the wire contract.
- No provider SDK behavior hidden from the lesson.
- Status, timeout, interruption, and JSON decisions are explicit.
- Useful when an API has unusual behavior or a framework lacks support.

### Costs

- More DTOs, conversion code, and tests.
- Provider API changes must be tracked directly.
- Cross-provider portability requires additional adapters.
- Retry, streaming, logging, and other features require careful implementation.

## LangChain4j path

`LangChain4jChatModelAdapter` converts JavaMind objects to the LangChain4j 1.20.1
chat model API:

```text
ChatRequest
    ↓
LangChain4j messages and request parameters
    ↓
LangChain4j ChatModel
    ↓
LangChain4j response metadata
    ↓
ChatResponse
```

### Strengths

- Less HTTP and JSON boilerplate in application code.
- Java-native abstractions shared across supported integrations.
- Framework features can be adopted intentionally in later phases.
- The adapter remains small and testable with a fake LangChain4j model.

### Costs

- Students must inspect documentation or source to understand framework behavior.
- Dependency upgrades can change APIs.
- Provider-specific capabilities can still leak if adapters are poorly designed.
- Framework use does not remove provider limits, errors, cost, or security duties.

## Why there is no universal winner

Raw HTTP is useful for learning internals and for maximum contract control.
LangChain4j is useful when a team wants Java-native AI abstractions and less
integration boilerplate. Project constraints—supported providers, required
features, upgrade policy, debugging needs, and team experience—determine which
trade-off is appropriate.

Response wording is not a meaningful implementation benchmark. Both paths can
send equivalent inputs to the same nondeterministic model. JavaMind's compare
mode reports response, usage, and latency so students can inspect mechanics; it
does not declare a quality winner.

## The stable boundary

Both implementations satisfy JavaMind's interface:

```java
ChatResponse generate(ChatRequest request);
```

This means future prompt construction can live above `ChatService`, and future
providers can live below `ChatModel`. Neither requires duplicating Phase 1's
token logic or rewriting the console flow.
