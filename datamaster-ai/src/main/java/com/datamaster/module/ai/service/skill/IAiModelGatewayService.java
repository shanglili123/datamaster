package com.datamaster.module.ai.service.skill;

public interface IAiModelGatewayService {

    String complete(String systemPrompt, String userPrompt);

    boolean available();
}

