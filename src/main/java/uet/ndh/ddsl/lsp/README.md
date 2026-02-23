# DDSL Language Server

A complete Language Server Protocol (LSP) implementation for the Domain-Driven Specification Language (DDSL).

## Features

### Syntax Highlighting (Semantic Tokens)
- Rich semantic token types for DDD constructs
- Keywords, types, annotations, identifiers
- Context-aware highlighting

### Auto-Completion
- Smart completions based on context
- Snippets for common patterns
- Documentation in completion items

### Hover Information
- Detailed documentation for keywords
- Type information for fields
- DDD concept explanations

### Diagnostics
- Lexical error detection
- Parse error reporting
- DDD best practice warnings
  - Aggregate without identity
  - Value object with identity (anti-pattern)
  - Large aggregate warnings
  - Stateful domain service hints

### Navigation
- Go-to-definition for types and fields
- Find all references
- Document symbols (outline)

### Code Actions
- Quick fixes for common issues
- Refactoring: Extract ValueObject
- Generate invariants/operations

### Formatting
- Consistent indentation
- Proper spacing
- Brace alignment

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Monaco Editor (React)                     │
│                 monaco-languageclient                        │
└─────────────────────────┬───────────────────────────────────┘
                          │ WebSocket (ws://localhost:8080/lsp)
                          │ JSON-RPC
┌─────────────────────────┴───────────────────────────────────┐
│                    Spring Boot Backend                       │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                 WebSocketConfig                         │ │
│  │            SimpleLspWebSocketHandler                    │ │
│  └────────────────────────┬───────────────────────────────┘ │
│                           │                                  │
│  ┌────────────────────────┴───────────────────────────────┐ │
│  │               DdslLanguageServer                        │ │
│  │  ┌──────────────────┐  ┌──────────────────┐            │ │
│  │  │TextDocumentService│  │ WorkspaceService │            │ │
│  │  └────────┬─────────┘  └──────────────────┘            │ │
│  └───────────┼────────────────────────────────────────────┘ │
│              │                                               │
│  ┌───────────┴───────────────────────────────────────────┐  │
│  │                    Providers                           │  │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐  │  │
│  │  │ Completion  │ │   Hover     │ │  Diagnostics    │  │  │
│  │  └─────────────┘ └─────────────┘ └─────────────────┘  │  │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐  │  │
│  │  │ Definition  │ │ Formatting  │ │  Code Actions   │  │  │
│  │  └─────────────┘ └─────────────┘ └─────────────────┘  │  │
│  │  ┌─────────────┐ ┌─────────────┐                      │  │
│  │  │ Symbols     │ │ Signature   │                      │  │
│  │  └─────────────┘ └─────────────┘                      │  │
│  └───────────────────────────────────────────────────────┘  │
│                           │                                  │
│  ┌────────────────────────┴───────────────────────────────┐ │
│  │                  DDSL Parser                            │ │
│  │  ┌──────────────────┐  ┌──────────────────────────┐    │ │
│  │  │     Scanner      │  │      DdslParser          │    │ │
│  │  │    (Lexer)       │  │  (Recursive Descent)     │    │ │
│  │  └──────────────────┘  └──────────────────────────┘    │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Setup

### Backend (Spring Boot)

1. Add dependencies to `build.gradle.kts`:
```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.23.1")
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc:0.23.1")
}
```

2. Start the server:
```bash
./gradlew bootRun
```

The LSP server will be available at:
- WebSocket: `ws://localhost:8080/lsp`
- REST API: `http://localhost:8080/api/lsp/*`

### Frontend (React + Monaco)

1. Install dependencies:
```bash
npm install @monaco-editor/react monaco-languageclient vscode-ws-jsonrpc
```

2. Create the editor component:

```tsx
import { useEffect, useRef } from 'react';
import * as monaco from 'monaco-editor';
import { MonacoLanguageClient } from 'monaco-languageclient';
import { WebSocketMessageReader, WebSocketMessageWriter, toSocket } from 'vscode-ws-jsonrpc';

function DdslEditor() {
  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);

  useEffect(() => {
    // Register DDSL language
    monaco.languages.register({ id: 'ddsl', extensions: ['.ddsl'] });

    // Create WebSocket connection
    const webSocket = new WebSocket('ws://localhost:8080/lsp');
    
    webSocket.onopen = () => {
      const socket = toSocket(webSocket);
      const reader = new WebSocketMessageReader(socket);
      const writer = new WebSocketMessageWriter(socket);

      const languageClient = new MonacoLanguageClient({
        name: 'DDSL Language Client',
        clientOptions: {
          documentSelector: [{ language: 'ddsl' }],
        },
        connectionProvider: {
          get: () => Promise.resolve({ reader, writer }),
        },
      });

      languageClient.start();
    };

    return () => webSocket.close();
  }, []);

  return (
    <div style={{ height: '100vh' }}>
      <monaco.Editor
        defaultLanguage="ddsl"
        defaultValue="BoundedContext Example {\n\n}"
        onMount={(editor) => { editorRef.current = editor; }}
        options={{
          'semanticHighlighting.enabled': true,
        }}
      />
    </div>
  );
}
```

## REST API (for testing/debugging)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/lsp/health` | GET | Health check |
| `/api/lsp/tokenize` | POST | Tokenize source code |
| `/api/lsp/semantic-tokens` | POST | Get semantic tokens |
| `/api/lsp/validate` | POST | Get diagnostics |
| `/api/lsp/completions` | POST | Get completions |
| `/api/lsp/hover` | POST | Get hover info |

### Example: Tokenize

```bash
curl -X POST http://localhost:8080/api/lsp/tokenize \
  -H "Content-Type: text/plain" \
  -d "Aggregate Order { @identity id: UUID }"
```

### Example: Validate

```bash
curl -X POST http://localhost:8080/api/lsp/validate \
  -H "Content-Type: text/plain" \
  -d "ValueObject Money { @identity id: UUID amount: Decimal }"
```

## Debugging

### Enable debug logging

Add to `application.properties`:
```properties
logging.level.uet.ndh.ddsl.lsp=DEBUG
```

### Check WebSocket messages

Use browser DevTools Network tab to inspect WebSocket frames.

### Test with curl

```bash
# Health check
curl http://localhost:8080/api/lsp/health

# Tokenize
curl -X POST http://localhost:8080/api/lsp/tokenize \
  -H "Content-Type: text/plain" \
  -d "BoundedContext Test {}"
```

## LSP Methods Supported

### Lifecycle
- `initialize` / `initialized`
- `shutdown` / `exit`

### Text Document
- `textDocument/didOpen`
- `textDocument/didChange`
- `textDocument/didClose`
- `textDocument/didSave`

### Language Features
- `textDocument/completion` / `completionItem/resolve`
- `textDocument/hover`
- `textDocument/definition`
- `textDocument/references`
- `textDocument/documentSymbol`
- `textDocument/formatting`
- `textDocument/semanticTokens/full`
- `textDocument/signatureHelp`
- `textDocument/codeAction`

### Workspace
- `workspace/didChangeConfiguration`
- `workspace/didChangeWatchedFiles`
- `workspace/symbol`
- `workspace/executeCommand`

## Semantic Token Types

| Token Type | DDSL Usage |
|------------|------------|
| namespace | BoundedContext |
| class | Aggregate, Entity, DomainService |
| struct | ValueObject |
| interface | Repository, Specification |
| event | DomainEvent |
| function | Factory |
| method | Operations, UseCase |
| keyword | when, require, then, etc. |
| type | String, Int, List, etc. |
| property | Field declarations |
| decorator | @identity, @required, etc. |
| string | String literals |
| number | Number literals |
| operator | =, >, <, etc. |

## Known Limitations

1. **Semantic Analysis**: Currently limited to lexical/syntactic analysis. Full semantic analysis (type checking, reference resolution) requires the complete AST.

2. **Cross-file References**: Go-to-definition and find-references work within a single file. Multi-file support would require workspace indexing.

3. **Rename Refactoring**: Not yet implemented. Would need full semantic analysis.

## Future Improvements

- [ ] Full semantic analysis integration
- [ ] Multi-file workspace support
- [ ] Rename refactoring
- [ ] Extract method/aggregate refactoring
- [ ] Import/export statement support
- [ ] Code lens for aggregate metrics
- [ ] Inlay hints for type inference
