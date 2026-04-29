package uet.ndh.ddsl.agent;

import java.util.List;

/**
 * Result record for NL → DSL translation.
 */
public record NlToDslResult(
        boolean success,
        String dsl,
        List<String> errors,
        int retrieverRetries,
        int synthesizerRetries,
        double retrievalQuality,
        String compilerFeedback
) {
    public static NlToDslResult from(DdslState state) {
        return new NlToDslResult(
                state.isSuccessful(),
                state.isSuccessful() ? state.finalDsl() : state.currentDsl(),
                state.errorLogs(),
                state.retrieverRetries(),
                state.synthesizerRetries(),
                state.retrievalQuality(),
                state.compilerFeedback()
        );
    }

    public static NlToDslResult failure(String errorMessage) {
        return new NlToDslResult(
                false, "", List.of(errorMessage), 0, 0, 0.0, ""
        );
    }
}