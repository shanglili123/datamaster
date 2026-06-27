package com.datamaster.module.assets.service.skill;

public interface IAiModelGatewayService {

    String complete(String systemPrompt, String userPrompt);

    boolean available();
}
