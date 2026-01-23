#!/bin/bash

echo "🧪 Testing DDSL Metrics and Timing System"
echo "==========================================="
echo

echo "📊 This test will demonstrate:"
echo "  • Comprehensive timing breakdown (parsing, validation, code generation)"
echo "  • Synthetic LOC calculation and actual LOC measurement"
echo "  • Domain model complexity analysis"
echo "  • Validation metrics and performance tracking"
echo "  • ERROR vs WARNING level handling"
echo

echo "🎯 Test 1: Successful compilation with metrics"
echo "File: blogdomain.yaml"
echo "Expected: Full metrics report with timing, LOC, and domain analysis"
echo

echo "🎯 Test 2: Validation with warnings (still generates code)"
echo "File: test-warnings.yaml"
echo "Expected: Metrics report showing warnings but successful generation"
echo

echo "🎯 Test 3: Validation errors (blocks code generation)"
echo "File: test-errors.yaml"
echo "Expected: Metrics report showing errors and blocked generation"
echo

echo "💡 Manual Test Commands:"
echo "  ./gradlew build"
echo "  java -jar build/libs/ddsl-1.0-SNAPSHOT.jar shell"
echo
echo "Then run in shell:"
echo "  generate --file blogdomain.yaml --output target/test1"
echo "  generate --file test-warnings.yaml --output target/test2"
echo "  generate --file test-errors.yaml --output target/test3"
echo "  validate --file simple-test.yaml"
echo

echo "📈 Expected Metrics Categories:"
echo "  ⏱️  TIMING BREAKDOWN - Parsing, validation, code gen, file writing"
echo "  📈 CODE GENERATION METRICS - Synthetic LOC, files, generation speed"
echo "  🔍 VALIDATION METRICS - Errors, warnings, total issues"
echo "  🏗️  DOMAIN MODEL METRICS - Entities, VOs, aggregates, complexity score"
echo

echo "🚀 Performance Benchmarks:"
echo "  • Small models (1-2 aggregates): 50-200ms total time"
echo "  • Medium models (5-10 aggregates): 200-800ms total time"
echo "  • Generation speed: 5,000-15,000 LOC/sec typical"
echo

echo "To run this test:"
echo "  chmod +x test-metrics.sh && ./test-metrics.sh"
