package com.datamaster.module.ai.model.dto.dbgpt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DbGptChatMessage {

    private String role;
    private String content;
}

