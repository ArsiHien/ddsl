#!/bin/bash

echo "🧪 Testing New Validation Behavior: Errors Block Code Generation, Warnings Don't"
echo "=" | tr " " "="
echo

# Test 1: File with warnings (should generate code)
echo "📝 Test 1: YAML with warnings only (should proceed with code generation)"
echo "File: test-warnings.yaml"
echo

# Test 2: File with errors (should block code generation)
echo "📝 Test 2: YAML with errors (should block code generation)"
echo "File: test-errors.yaml"
echo

echo "ℹ️  To run these tests manually:"
echo "   ./gradlew build"
echo "   java -jar build/libs/ddsl-1.0-SNAPSHOT.jar shell"
echo "   Then run:"
echo "     generate --file test-warnings.yaml --output target/test-warnings"
echo "     generate --file test-errors.yaml --output target/test-errors"
echo
echo "Expected Results:"
echo "  ✅ test-warnings.yaml: Code generation succeeds with warnings shown"
echo "  ❌ test-errors.yaml: Code generation blocked due to errors"
