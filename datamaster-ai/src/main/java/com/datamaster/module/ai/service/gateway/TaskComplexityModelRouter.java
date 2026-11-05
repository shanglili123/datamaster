package com.datamaster.module.ai.service.gateway;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.router.ChatModelRouter;
import dev.langchain4j.model.chat.router.ChatModelRoutingRequest;
import dev.langchain4j.model.chat.router.ChatModelRoutingResult;

/** Deterministic task/size heuristic, not an LLM judge or a guarantee of answer quality/cost. */
public final class TaskComplexityModelRouter implements ChatModelRouter {
    private final String scene;
    private final int complexInputCharacters;

    public TaskComplexityModelRouter(String scene, int complexInputCharacters) {
        if (complexInputCharacters < 1) throw new IllegalArgumentException("路由输入长度阈值必须大于零");
        this.scene = scene;
        this.complexInputCharacters = complexInputCharacters;
    }

    @Override
    public ChatModelRoutingResult route(ChatModelRoutingRequest request) {
        boolean advanced = scene != null && (scene.startsWith("DECISION") || scene.startsWith("ACTION_")
                || "ONTOLOGY_GENERATE".equals(scene) || "ETL_OPS_ANALYSIS".equals(scene));
        advanced |= request.chatRequest().responseFormat() != null;
        advanced |= request.chatRequest().toolSpecifications() != null && !request.chatRequest().toolSpecifications().isEmpty();
        long size = 0;
        for (ChatMessage message : request.chatRequest().messages()) {
            if (message instanceof SystemMessage) size += ((SystemMessage) message).text().length();
            else if (message instanceof UserMessage) {
                for (dev.langchain4j.data.message.Content content : ((UserMessage) message).contents()) {
                    if (content instanceof TextContent) size += ((TextContent) content).text().length();
                    else advanced = true;
                }
            } else if (message instanceof AiMessage) {
                AiMessage ai = (AiMessage) message;
                if (ai.text() != null) size += ai.text().length();
                advanced |= ai.hasToolExecutionRequests();
            } else if (message instanceof ToolExecutionResultMessage) {
                ToolExecutionResultMessage result = (ToolExecutionResultMessage) message;
                if (result.text() != null) size += result.text().length();
                advanced = true;
            } else advanced = true;
            if (size >= complexInputCharacters) advanced = true;
        }
        String route = advanced ? "advanced" : "economy";
        return request.routes().stream().anyMatch(candidate -> route.equals(candidate.name()))
                ? ChatModelRoutingResult.route(route) : ChatModelRoutingResult.defaultRoute();
    }
}
