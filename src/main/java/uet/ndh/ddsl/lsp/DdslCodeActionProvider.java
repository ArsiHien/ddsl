package uet.ndh.ddsl.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import uet.ndh.ddsl.lsp.core.DdslCommandIds;

import java.util.*;

/**
 * Code Action provider for DDSL Language Server.
 * 
 * Provides quick fixes and refactoring actions:
 * - Add missing @identity annotation
 * - Convert Entity to ValueObject (and vice versa)
 * - Extract ValueObject from fields
 * - Add missing invariants
 * - Generate boilerplate code
 */
public class DdslCodeActionProvider {
    
    /**
     * Get code actions for the given range and context.
     */
    public static List<Either<Command, CodeAction>> getCodeActions(
            String uri, String content, Range range, CodeActionContext context) {
        
        List<Either<Command, CodeAction>> actions = new ArrayList<>();
        
        // Get diagnostics in the range
        List<Diagnostic> diagnostics = context.getDiagnostics();
        
        for (Diagnostic diagnostic : diagnostics) {
            String code = diagnostic.getCode() != null ? 
                diagnostic.getCode().getLeft() : null;
            
            if (code == null) continue;
            
            switch (code) {
                case "ddd-aggregate-identity" -> {
                    // Quick fix: Add @identity annotation
                    actions.add(Either.forRight(createAddIdentityAction(uri, diagnostic)));
                }
                case "ddd-value-object-identity" -> {
                    // Quick fix: Remove @identity or convert to Entity
                    actions.add(Either.forRight(createRemoveIdentityAction(uri, diagnostic)));
                    actions.add(Either.forRight(createConvertToEntityAction(uri, diagnostic)));
                }
                case "ddd-aggregate-size" -> {
                    // Suggestion: Extract ValueObject
                    actions.add(Either.forRight(createExtractValueObjectAction(uri, diagnostic)));
                }
            }
        }
        
        // Add refactoring actions based on selection
        if (isNonEmptyRange(range)) {
            // Extract selection to ValueObject
            actions.add(Either.forRight(createExtractSelectionAction(uri, range)));
        }
        
        // Add source actions
        actions.add(Either.forRight(createGenerateInvariantsAction(uri)));
        actions.add(Either.forRight(createGenerateOperationsAction(uri)));
        
        return actions;
    }
    
    /**
     * Create action to add @identity annotation.
     */
    private static CodeAction createAddIdentityAction(String uri, Diagnostic diagnostic) {
        CodeAction action = new CodeAction();
        action.setTitle("Add @identity field");
        action.setKind(CodeActionKind.QuickFix);
        action.setDiagnostics(List.of(diagnostic));
        
        // Create workspace edit to add identity field
        WorkspaceEdit edit = new WorkspaceEdit();
        
        // Calculate insert position (after opening brace)
        Position insertPos = new Position(
            diagnostic.getRange().getStart().getLine() + 1, 
            4  // Indent
        );
        
        TextEdit textEdit = new TextEdit(
            new Range(insertPos, insertPos),
            "@identity id: UUID\n    "
        );
        
        edit.setChanges(Map.of(uri, List.of(textEdit)));
        action.setEdit(edit);
        
        return action;
    }
    
    /**
     * Create action to remove @identity annotation.
     */
    private static CodeAction createRemoveIdentityAction(String uri, Diagnostic diagnostic) {
        CodeAction action = new CodeAction();
        action.setTitle("Remove @identity annotation");
        action.setKind(CodeActionKind.QuickFix);
        action.setDiagnostics(List.of(diagnostic));
        
        // Create workspace edit to remove the annotation
        WorkspaceEdit edit = new WorkspaceEdit();
        
        Range removeRange = diagnostic.getRange();
        TextEdit textEdit = new TextEdit(removeRange, "");
        
        edit.setChanges(Map.of(uri, List.of(textEdit)));
        action.setEdit(edit);
        
        return action;
    }
    
    /**
     * Create action to convert ValueObject to Entity.
     */
    private static CodeAction createConvertToEntityAction(String uri, Diagnostic diagnostic) {
        CodeAction action = new CodeAction();
        action.setTitle("Convert to Entity");
        action.setKind(CodeActionKind.RefactorRewrite);
        action.setDiagnostics(List.of(diagnostic));
        
        // This would need to find the ValueObject keyword and replace it
        // For now, we'll use a command that the client can execute
        Command command = new Command();
        command.setTitle("Convert to Entity");
        command.setCommand(DdslCommandIds.CONVERT_TO_ENTITY);
        command.setArguments(List.of(uri, diagnostic.getRange()));
        
        action.setCommand(command);
        
        return action;
    }
    
    /**
     * Create action to extract ValueObject from fields.
     */
    private static CodeAction createExtractValueObjectAction(String uri, Diagnostic diagnostic) {
        CodeAction action = new CodeAction();
        action.setTitle("Extract fields to ValueObject");
        action.setKind(CodeActionKind.RefactorExtract);
        action.setDiagnostics(List.of(diagnostic));
        
        // This requires user interaction to select fields
        Command command = new Command();
        command.setTitle("Extract ValueObject");
        command.setCommand(DdslCommandIds.EXTRACT_VALUE_OBJECT);
        command.setArguments(List.of(uri, diagnostic.getRange()));
        
        action.setCommand(command);
        
        return action;
    }
    
    /**
     * Create action to extract selection to ValueObject.
     */
    private static CodeAction createExtractSelectionAction(String uri, Range range) {
        CodeAction action = new CodeAction();
        action.setTitle("Extract to ValueObject");
        action.setKind(CodeActionKind.RefactorExtract);
        
        Command command = new Command();
        command.setTitle("Extract to ValueObject");
        command.setCommand(DdslCommandIds.EXTRACT_TO_VALUE_OBJECT);
        command.setArguments(List.of(uri, range));
        
        action.setCommand(command);
        
        return action;
    }
    
    /**
     * Create action to generate invariants.
     */
    private static CodeAction createGenerateInvariantsAction(String uri) {
        CodeAction action = new CodeAction();
        action.setTitle("Generate invariants block");
        action.setKind(CodeActionKind.Source);
        
        Command command = new Command();
        command.setTitle("Generate Invariants");
        command.setCommand(DdslCommandIds.GENERATE_INVARIANTS);
        command.setArguments(List.of(uri));
        
        action.setCommand(command);
        
        return action;
    }
    
    /**
     * Create action to generate operations.
     */
    private static CodeAction createGenerateOperationsAction(String uri) {
        CodeAction action = new CodeAction();
        action.setTitle("Generate operations block");
        action.setKind(CodeActionKind.Source);
        
        Command command = new Command();
        command.setTitle("Generate Operations");
        command.setCommand(DdslCommandIds.GENERATE_OPERATIONS);
        command.setArguments(List.of(uri));
        
        action.setCommand(command);
        
        return action;
    }
    
    /**
     * Check if a range is non-empty (user selected something).
     */
    private static boolean isNonEmptyRange(Range range) {
        return range != null &&
               (range.getStart().getLine() != range.getEnd().getLine() ||
                range.getStart().getCharacter() != range.getEnd().getCharacter());
    }
}
