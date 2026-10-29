package com.datamaster.module.ai.agent.runtime;

import com.datamaster.module.ai.agent.tool.AgentToolResult;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 自主 Agent 调度结果。 */
@Data
public class AgentRunResponse {

    private boolean success;
    private String status;
    private String message;
    private String text;
    private String conversationId;
    private Long sessionId;
    private String operationId;
    private String delegateOperationId;
    private Integer progress;
    private String currentStep;
    private String confirmationType;
    private String confirmationMessage;
    private com.datamaster.module.ai.agent.decision.DecisionModels.Proposal decision;
    private List<String> requiredFields = new ArrayList<>();
    private List<String> output = new ArrayList<>();
    private List<AgentToolResult> toolResults = new ArrayList<>();

    public static AgentRunResponse failure(String message) {
        AgentRunResponse response = new AgentRunResponse();
        response.success = false;
        response.status = AgentToolResult.FAILED;
        response.message = message;
        return response;
    }
}
