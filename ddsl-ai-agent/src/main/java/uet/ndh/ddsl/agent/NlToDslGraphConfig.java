package uet.ndh.ddsl.agent;

import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.StateGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uet.ndh.ddsl.agent.node.JudgeNode;
import uet.ndh.ddsl.agent.node.SynthesizerNode;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Defines the LangGraph4j state graph for the NL → DDSL self-healing pipeline.
 * <p>
 * <b>Graph topology (2 nodes):</b>
 * <pre>
 *   START → synthesizer → judge ─┬─→ END   (valid DSL or max retries)
 *                ▲                │
 *                └────────────────┘  (parser errors → retry)
 * </pre>
 */
@Configuration
@Slf4j
public class NlToDslGraphConfig {

    public static final String NODE_SYNTHESIZER = "synthesizer";
    public static final String NODE_JUDGE = "judge";

    @Bean
    public StateGraph<DslState> nlToDslGraph(
            SynthesizerNode synthesizerNode,
            JudgeNode judgeNode
    ) throws Exception {

        var graph = new StateGraph<>(DslState.SCHEMA, DslState::new)
                .addNode(NODE_SYNTHESIZER, node_async(synthesizerNode))
                .addNode(NODE_JUDGE, node_async(judgeNode))
                .addEdge(START, NODE_SYNTHESIZER)
                .addEdge(NODE_SYNTHESIZER, NODE_JUDGE)
                .addConditionalEdges(
                        NODE_JUDGE,
                        state -> {
                            if (state.isSuccessful()) {
                                log.info("Graph: DSL is valid — finishing");
                                return CompletableFuture.completedFuture("finish");
                            }
                            if (state.retryCount() >= state.maxRetries()) {
                                log.warn("Graph: max retries ({}) reached — finishing with errors",
                                        state.maxRetries());
                                return CompletableFuture.completedFuture("finish");
                            }
                            log.info("Graph: errors found — looping back to synthesizer (retry={})",
                                    state.retryCount());
                            return CompletableFuture.completedFuture("retry");
                        },
                        Map.of("finish", END, "retry", NODE_SYNTHESIZER)
                );

        return graph;
    }
}
