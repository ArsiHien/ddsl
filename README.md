# Domain-Driven Design DSL (DDSL)

A sophisticated Domain Specific Language for generating Domain-Driven Design (DDD) code with comprehensive validation, metrics tracking, and code generation capabilities.

## 🚀 Features

### 🎯 Domain Modeling
- **YAML-based DSL** for defining domain models using DDD tactical patterns
- **Bounded Contexts** with aggregates, entities, value objects, and services
- **Rich Type System** with Java type mapping and validation
- **Comprehensive Validation** with 13+ DDD rules enforcing best practices

### 📊 Metrics & Performance Tracking
- **Compilation Timing** - Track parsing, validation, code generation, and file writing phases
- **Synthetic LOC Calculation** - Estimate and measure generated lines of code
- **Domain Complexity Analysis** - Automatic complexity scoring of domain models
- **Validation Metrics** - Detailed error and warning categorization
- **Performance Metrics** - Generation speed (LOC/sec) and throughput analysis

### 🔍 Validation System
- **ERROR-level issues** block code generation
- **WARNING-level issues** allow code generation but provide feedback
- **Location-accurate error reporting** with line and column information
- **DDD Pattern Enforcement** including:
  - Entity identity and immutability rules
  - Value object immutability and no-identity rules  
  - Aggregate size limits and reference-by-ID-only rules
  - Domain event naming and structure rules
  - Repository and application service patterns

### 🔨 Code Generation
- **Pure Java code generation** (Spring Boot compatible)
- **Lombok integration** for clean, concise generated code
- **Proper package structure** following DDD conventions
- **Complete class hierarchy** including entities, VOs, services, repositories

## 🛠️ Usage

### Command Line Interface

#### Generate Code
```bash
# Generate Java code from YAML specification
./ddslc generate --file model.yaml --target java --output target/generated-sources

# Or using the built JAR
java -jar ddsl-1.0-SNAPSHOT.jar shell
generate --file model.yaml --output target/generated-sources
```

#### Validate Only
```bash
# Validate domain model without generating code
./ddslc validate --file model.yaml

# Or using shell
validate --file model.yaml
```

### Sample Output with Metrics
```
🚀 Starting DDSL compilation...
📄 Input file: blogdomain.yaml
🎯 Target: java
📁 Output: target/generated-sources

🔄 Starting PARSING...
✅ Completed PARSING in 45 ms
🔄 Starting DOMAIN_ANALYSIS...
✅ Completed DOMAIN_ANALYSIS in 12 ms
🔄 Starting VALIDATION...

🔍 Running DDD Tactical Design Validation...
📋 Checking 13 validation rules

  ✓ ENTITY_MUST_HAVE_IDENTITY: Entity Must Have Identity... ✅ PASS
  ✓ VALUE_OBJECT_IMMUTABLE: Value Object Immutable... ❌ 2 issue(s) found
  ✓ AGGREGATE_ONE_ROOT: Aggregate Has One Root... ✅ PASS
  ...

✅ Completed VALIDATION in 89 ms
🔄 Starting CODE_GENERATION...
🔨 Generating code...
✅ Completed CODE_GENERATION in 234 ms
🔄 Starting FILE_WRITING...
✅ Completed FILE_WRITING in 67 ms

============================================================
📊 COMPILATION METRICS REPORT
============================================================

⏱️  TIMING BREAKDOWN:
  CODE_GENERATION     :    234 ms
  VALIDATION          :     89 ms
  FILE_WRITING        :     67 ms
  PARSING             :     45 ms
  DOMAIN_ANALYSIS     :     12 ms
  TOTAL_TIME          :    447 ms

📈 CODE GENERATION METRICS:
  Synthetic LOC       :  3,247 lines
  Files Generated     :     18 files
  Generation Speed    :  7,265.5 LOC/sec

🔍 VALIDATION METRICS:
  Errors              :      0 issues
  Warnings            :      3 issues
  Total Issues        :      3 total

🏗️  DOMAIN MODEL METRICS:
  BOUNDED CONTEXTS    : 2
  TOTAL ENTITIES      : 8
  TOTAL VALUE OBJECTS : 12
  TOTAL AGGREGATES    : 4
  TOTAL REPOSITORIES  : 4
  COMPLEXITY SCORE    : 156.00
============================================================

✅ Code generation completed successfully!

📊 Quick Metrics Summary:
  ⏱️  Total Time: 447 ms
  📄 Generated Files: 18
  📏 Synthetic LOC: 3,247 lines
  📐 Actual LOC: 3,156 lines
```

## 📋 YAML Specification Format

```yaml
model:
  name: "BlogSystem"
  basePackage: "com.example.blog"
  version: "1.0.0"

boundedContexts:
  - name: "BlogContext"
    package: "blog"
    
    valueObjects:
      - name: "PostTitle"
        fields:
          - name: "value"
            type: "String"
            final: true
            constraints:
              - type: "NOT_EMPTY"

    aggregates:
      - name: "Post"
        root:
          name: "Post"
          isAggregateRoot: true
          identityField:
            name: "postId"
            type: "UUID"
          fields:
            - name: "title"
              type: "PostTitle"
              final: true
    
    repositories:
      - name: "PostRepository"
        aggregateType: "Post"
        idType: "UUID"
```

## 🏗️ Architecture

### Core Components
- **Parser** (`parser/`) - YAML to AST conversion with location tracking
- **Core** (`core/`) - AST nodes, type system, and validation framework  
- **Validator** (`validator/`) - DDD tactical design pattern enforcement
- **Compiler** (`compiler/`) - Orchestration with metrics tracking
- **CodeGen** (`codegen/`) - Java code generation with synthetic LOC calculation
- **Shell** (`shell/`) - Interactive command-line interface

### Validation Rules (13+ Implemented)
1. **Entity Rules** - Identity, equality, immutability
2. **Value Object Rules** - Immutability, no identity fields
3. **Aggregate Rules** - Single root, ID-only references, size limits
4. **Domain Event Rules** - Immutability, past tense naming, required fields  
5. **Repository Rules** - Per aggregate root only
6. **Application Service Rules** - No business logic validation

## 🔧 Development

### Build
```bash
./gradlew build
```

### Test
```bash
./gradlew test
```

### Run Shell
```bash
./gradlew bootRun
```

## 📊 Metrics Categories

### Timing Metrics
- **PARSING** - YAML to AST conversion time
- **DOMAIN_ANALYSIS** - Domain model structure analysis
- **VALIDATION** - DDD rules validation time
- **CODE_GENERATION** - AST to Java code generation
- **FILE_WRITING** - Writing generated files to disk

### Code Metrics
- **Synthetic LOC** - Estimated lines of code before generation
- **Actual LOC** - Measured lines in generated files (excluding comments/empty lines)
- **Files Generated** - Total number of generated .java files
- **Generation Speed** - Lines of code generated per second

### Domain Metrics
- **Bounded Contexts** - Number of domain contexts
- **Entities/Value Objects/Aggregates** - Count by type
- **Complexity Score** - Weighted complexity calculation
- **Validation Issues** - Errors vs warnings breakdown

### Performance Benchmarks
- **Small Models** (1-2 aggregates): ~50-200ms total time
- **Medium Models** (5-10 aggregates): ~200-800ms total time  
- **Large Models** (20+ aggregates): ~800ms+ total time
- **Generation Speed**: Typically 5,000-15,000 LOC/sec depending on model complexity

---

**Built with Spring Boot, Lombok, and comprehensive DDD tactical design validation.**
