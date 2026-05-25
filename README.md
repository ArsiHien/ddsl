# DDSL

DDSL is a domain-specific language for modeling Domain-Driven Design systems and generating Java code from those models. The repository includes the core compiler, a Language Server Protocol server, an AI-assisted DSL generation backend, and generated-code test suites.

## Project Structure

- `ddsl-core` - lexer, parser, AST, symbol/type resolution, validation, and Java code generation.
- `ddsl-lsp-server` - LSP server for editor integrations, with fat JAR and GraalVM native image targets.
- `ddsl-ai-agent` - Spring Boot backend that uses OpenRouter, LangGraph4j, Qdrant, and an MCP syntax judge.
- `ddsl-generated-tests` - JUnit tests for Java code generated from DDSL samples.
- `samples/` - example `.ddsl` domain models.

## Requirements

- Java 25. Use GraalVM Java 25 if you plan to build the native LSP binary.
- Docker, for running Qdrant locally.
- An OpenRouter API key, required by `ddsl-ai-agent`.

## Setup

1. Clone the repository and enter the project directory.

   ```bash
   git clone [<repo-url>](https://github.com/ArsiHien/ddsl.git)
   cd ddsl
   ```

2. Configure your OpenRouter API key.

   Prefer an environment variable:

   ```bash
   export OPENROUTER_API_KEY=your_openrouter_api_key_here
   ```

   You can also set it in `ddsl-ai-agent/src/main/resources/application-local.properties` for local development.

3. Start Qdrant before running the AI agent.

   ```bash
   docker compose up -d
   ```

   Qdrant listens on `localhost:6333` for REST and `localhost:6334` for gRPC.

## Build and Test

Build all modules:

```bash
./gradlew build
```

Run tests:

```bash
./gradlew test
```

Run only the generated-code tests:

```bash
./gradlew :ddsl-generated-tests:test
```

Run the AI agent test runner with local configuration:

```bash
./gradlew :ddsl-ai-agent:test --tests "NlToDslTestRunner" -Dspring.profiles.active=local
```

## Run

Start the AI agent Spring Boot server:

```bash
./gradlew bootRun
```

Build the LSP server fat JAR:

```bash
./gradlew :ddsl-lsp-server:shadowJar
```

Build the native LSP binary with GraalVM:

```bash
./gradlew :ddsl-lsp-server:nativeCompile
```

The native binary is generated as `ddsl-lsp`.
