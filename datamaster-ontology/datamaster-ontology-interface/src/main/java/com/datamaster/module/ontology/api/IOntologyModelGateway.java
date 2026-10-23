package com.datamaster.module.ontology.api;

/** Model completion capability supplied by the AI module at runtime. */
public interface IOntologyModelGateway {

    String complete(String systemPrompt, String userPrompt);

    boolean available();
}
