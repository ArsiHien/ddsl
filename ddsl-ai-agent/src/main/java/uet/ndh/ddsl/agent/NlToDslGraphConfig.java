package uet.ndh.ddsl.agent;

import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uet.ndh.ddsl.agent.node.JudgeNode;
import uet.ndh.ddsl.agent.node.RetrieverNode;
import uet.ndh.ddsl.agent.node.SynthesizerNode;
import uet.ndh.ddsl.agent.DdslState;
import org.bsc.langgraph4j.state.Channel;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * LangGraph4j configuration for the NL → DDSL pipeline.
 * <p>
* Flow:
     * <pre>
     * START → Retriever → [Quality Check]
     *                         ↓ Good
     *                       Synthesizer → Judge → {Valid?}
     *                          ↑ No          ↓ Yes
     *                       [Retry]      Return DSL
 * </pre>
 * <p>
 * Retry limits per agent: 2
 */
@Configuration
@Slf4j
public class NlToDslGraphConfig {

    public static final String NODE_RETRIEVER = "retriever";
    public static final String NODE_SYNTHESIZER = "synthesizer";
    public static final String NODE_JUDGE = "judge";

    @Bean
    public StateGraph<DdslState> nlToDslGraph(
            RetrieverNode retrieverNode,
            SynthesizerNode synthesizerNode,
            JudgeNode judgeNode
    ) throws Exception {

        Map<String, Channel<?>> schema = Map.of();
        var graph = new StateGraph<>(schema, DdslState::from)
                // Nodes
                .addNode(NODE_RETRIEVER, node_async(retrieverNode))
                .addNode(NODE_SYNTHESIZER, node_async(synthesizerNode))
                .addNode(NODE_JUDGE, node_async(judgeNode))
                
                // Start
                .addEdge(START, NODE_RETRIEVER)
                
                // Retriever → Quality Check
                .addConditionalEdges(
                    NODE_RETRIEVER,
                    state -> {
                        double quality = state.retrievalQuality();
                        int retries = state.retrieverRetries();
                        int maxRetries = state.maxRetries();
                        
                        if (quality >= 0.6) {
                            log.info("Retriever: quality good ({}), proceeding", quality);
                            return CompletableFuture.completedFuture("good");
                        }
                        if (retries < maxRetries) {
                            log.info("Retriever: quality low ({}), retrying ({}/{})", 
                                quality, retries, maxRetries);
                            return CompletableFuture.completedFuture("retry");
                        }
                        log.warn("Retriever: quality low after max retries, proceeding with low quality");
                        return CompletableFuture.completedFuture("good");
                    },
                    Map.of("good", NODE_SYNTHESIZER, "retry", NODE_RETRIEVER)
                )
                
                // Synthesizer → Judge
                .addEdge(NODE_SYNTHESIZER, NODE_JUDGE)
                
                // Judge → Validation Check
                .addConditionalEdges(
                    NODE_JUDGE,
                    state -> {
                        boolean valid = state.isSuccessful();
                        int retries = state.synthesizerRetries();
                        int maxRetries = state.maxRetries();
                        
                        if (valid) {
                            log.info("Judge: DSL valid, completing");
                            return CompletableFuture.completedFuture("valid");
                        }
                        if (retries < maxRetries) {
                            log.info("Judge: DSL invalid, retrying synthesis ({}/{})", 
                                retries, maxRetries);
                            return CompletableFuture.completedFuture("retry");
                        }
                        log.warn("Judge: DSL invalid after max retries, returning best effort");
                        return CompletableFuture.completedFuture("fail");
                    },
                    Map.of("valid", END, "retry", NODE_SYNTHESIZER, "fail", END)
                );

        return graph;
    }
}
