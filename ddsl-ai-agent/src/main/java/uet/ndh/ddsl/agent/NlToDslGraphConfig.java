package uet.ndh.ddsl.agent;

import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uet.ndh.ddsl.agent.node.JudgeNode;
import uet.ndh.ddsl.agent.node.OrchestratorNode;
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
     * START → Orchestrator → Retriever → Orchestrator → Synthesizer
     *          ↑                                          ↓
     *          └──────── Judge ← Orchestrator ←───────────┘
     *
     * Orchestrator creates chunk plan, merges chunks, and scopes repair loops.
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
    public static final String NODE_ORCHESTRATOR = "orchestrator";

    @Bean
    public StateGraph<DdslState> nlToDslGraph(
            RetrieverNode retrieverNode,
            SynthesizerNode synthesizerNode,
            JudgeNode judgeNode,
            OrchestratorNode orchestratorNode
    ) throws Exception {

        Map<String, Channel<?>> schema = DdslState.SCHEMA;
        var graph = new StateGraph<>(schema, DdslState::from)
                // Nodes
                .addNode(NODE_ORCHESTRATOR, node_async(orchestratorNode))
                .addNode(NODE_RETRIEVER, node_async(retrieverNode))
                .addNode(NODE_SYNTHESIZER, node_async(synthesizerNode))
                .addNode(NODE_JUDGE, node_async(judgeNode))
                
                // Start
                .addEdge(START, NODE_ORCHESTRATOR)

                .addConditionalEdges(
                        NODE_ORCHESTRATOR,
                        state -> CompletableFuture.completedFuture(state.orchestratorRoute()),
                        Map.of(
                                OrchestratorNode.ROUTE_RETRIEVER, NODE_RETRIEVER,
                                OrchestratorNode.ROUTE_SYNTHESIZER, NODE_SYNTHESIZER,
                                OrchestratorNode.ROUTE_JUDGE, NODE_JUDGE,
                                OrchestratorNode.ROUTE_END, END
                        )
                )

                .addConditionalEdges(
                        NODE_RETRIEVER,
                        state -> {
                            double quality = state.retrievalQuality();
                            int retries = state.retrieverRetries();
                            int maxRetries = state.maxRetries();

                            if (quality >= 0.6 || retries >= maxRetries) {
                                if (quality < 0.6) {
                                    log.warn("Retriever: low quality ({}) after retry budget, proceeding", quality);
                                }
                                return CompletableFuture.completedFuture("continue");
                            }
                            log.info("Retriever: quality low ({}), retrying ({}/{})", quality, retries, maxRetries);
                            return CompletableFuture.completedFuture("retry");
                        },
                        Map.of("continue", NODE_ORCHESTRATOR, "retry", NODE_RETRIEVER)
                )

                .addEdge(NODE_SYNTHESIZER, NODE_ORCHESTRATOR)
                .addEdge(NODE_JUDGE, NODE_ORCHESTRATOR);

        return graph;
    }
}
