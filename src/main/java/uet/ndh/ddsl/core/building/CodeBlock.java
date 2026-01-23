package uet.ndh.ddsl.core.building;

import uet.ndh.ddsl.core.building.statement.Statement;
import uet.ndh.ddsl.core.building.statement.RawStatement;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a block of code statements.
 */
public record CodeBlock(List<Statement> statements) {
    public CodeBlock() {
        this(new ArrayList<>());
    }

    public CodeBlock(List<Statement> statements) {
        this.statements = new ArrayList<>(statements);
    }

    @Override
    public List<Statement> statements() {
        return new ArrayList<>(statements);
    }

    public void addStatement(Statement statement) {
        statements.add(statement);
    }

    public void addStatements(List<Statement> statementsToAdd) {
        statements.addAll(statementsToAdd);
    }

    public void addRawCode(String code) {
        statements.add(new RawStatement(code));
    }

    public String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (Statement statement : statements) {
            String statementCode = statement.generateCode();
            if (statementCode != null && !statementCode.trim().isEmpty()) {
                // Add proper indentation and handle multi-line statements
                String[] lines = statementCode.split("\n");
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        sb.append("        ").append(line).append("\n");
                    }
                }
            }
        }
        return sb.toString();
    }

    public boolean isEmpty() {
        return statements.isEmpty();
    }

    /**
     * Create a copy of this code block.
     */
    public CodeBlock copy() {
        CodeBlock copy = new CodeBlock();
        for (Statement statement : this.statements) {
            copy.addStatement(statement.copy());
        }
        return copy;
    }

    public void clear() {
        statements.clear();
    }
}
