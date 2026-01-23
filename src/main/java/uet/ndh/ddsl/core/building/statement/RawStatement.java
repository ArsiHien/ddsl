package uet.ndh.ddsl.core.building.statement;

/**
 * Raw statement for backward compatibility and custom code.
 */
public class RawStatement extends Statement {
    private final String code;

    public RawStatement(String code) {
        this.code = code;
    }

    @Override
    public String generateCode() {
        return code;
    }

    @Override
    public Statement copy() {
        return new RawStatement(this.code);
    }
}
