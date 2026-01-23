package uet.ndh.ddsl.codegen;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uet.ndh.ddsl.core.CodeGenVisitor;
import uet.ndh.ddsl.core.codegen.*;
import uet.ndh.ddsl.core.model.DomainModel;

/**
 * Generates Java code from domain model AST using templates.
 */
@Service
@RequiredArgsConstructor
public class JavaCodeGenerator {

    public CodeArtifacts generate(DomainModel model) {
        JavaCodeGenVisitor visitor = new JavaCodeGenVisitor();
        model.accept(visitor);
        return visitor.getCodeArtifacts();
    }
}
