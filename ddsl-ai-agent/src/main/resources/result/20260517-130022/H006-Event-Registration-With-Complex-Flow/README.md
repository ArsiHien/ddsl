# H006 - Event Registration With Complex Flow

- Category: `hard`
- Success: `true`
- Syntax valid: `true`
- Total time: `75115 ms`
- Steps: `21`
- Synthesis artifacts: `4`
- Judge artifacts: `1`

## Node Timings

- `__START__`: iterations=`1`, totalMs=`1`
- `orchestrator`: iterations=`10`, totalMs=`75101`
- `retriever`: iterations=`4`, totalMs=`1`
- `synthesizer`: iterations=`4`, totalMs=`2`
- `judge`: iterations=`1`, totalMs=`1`
- `__END__`: iterations=`1`, totalMs=`0`

## Files

- `input.txt`
- `final.ddsl`
- `final-numbered.ddsl`
- `compiler-feedback.txt`
- `trace.json`
- `synthesis-artifacts.json`
- `judge-artifacts.json`
- `llm-*.ddsl`: raw LLM output per Synthesizer call
- `judge-*-compiler-output.json`: raw compiler/MCP output per Judge call
- `judge-*-merged.ddsl`: merged DSL sent to Judge
