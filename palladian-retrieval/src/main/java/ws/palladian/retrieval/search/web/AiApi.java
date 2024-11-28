package ws.palladian.retrieval.search.web;

import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonObject;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This interface is used to mark classes that are wrappers for AI APIs.
 *
 * @author David Urbansky
 * @since 10.04.2024 at 22:02
 **/
public abstract class AiApi {

    public String chat(String prompt) throws Exception {
        return chat(prompt, 1.0, null);
    }

    public String chat(String prompt, double temperature, AtomicInteger usedTokens) throws Exception {
        return chat(makeMessages(prompt), temperature, usedTokens);
    }

    public abstract String chat(JsonArray messages, double temperature, AtomicInteger usedTokens) throws Exception;

    public String chat(String prompt, double temperature, AtomicInteger usedTokens, String modelName) throws Exception {
        return chat(makeMessages(prompt), temperature, usedTokens, modelName, null, null);
    }

    public String chat(JsonArray messages, double temperature, AtomicInteger usedTokens, String modelName) throws Exception {
        return chat(messages, temperature, usedTokens, modelName, null, null);
    }

    public String chat(String prompt, double temperature, AtomicInteger usedTokens, String modelName, Integer maxTokens) throws Exception {
        return chat(makeMessages(prompt), temperature, usedTokens, modelName, maxTokens, null);
    }

    public String chat(String prompt, double temperature, AtomicInteger usedTokens, String modelName, Integer maxTokens, JsonObject responseSchema) throws Exception {
        return chat(makeMessages(prompt), temperature, usedTokens, modelName, maxTokens, responseSchema);
    }

    public String chat(JsonArray messages, double temperature, AtomicInteger usedTokens, String modelName, Integer maxTokens) throws Exception {
        return chat(messages, temperature, usedTokens, modelName, maxTokens, null);
    }

    public String chat(AiParameters params) throws Exception {
        return chat(params.getMessages(), params.getTemperature(), params.getUsedTokens(), params.getModelName(), params.getMaxTokens(), params.getResponseSchema());
    }

    public abstract String chat(JsonArray messages, double temperature, AtomicInteger usedTokens, String modelName, Integer maxTokens, JsonObject responseSchema) throws Exception;

    private JsonArray makeMessages(String prompt) {
        JsonObject message = new JsonObject();
        message.put("role", "user");
        message.put("content", prompt);
        JsonArray messages = new JsonArray();
        messages.add(message);
        return messages;
    }
}
