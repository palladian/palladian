package ws.palladian.retrieval.search.web;

import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonObject;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ai settings.
 *
 * @author David Urbansky
 * @since 28.11.2024 at 08:19
 **/
public class AiParameters {
    private String prompt;
    private JsonArray messages;
    private double temperature = 1.;
    private AtomicInteger usedTokens;
    private String modelName;
    private Integer maxTokens;
    private JsonObject responseSchema;

    public AiParameters setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    public AiParameters setMessages(JsonArray messages) {
        this.messages = messages;
        return this;
    }

    public AiParameters setUsedTokens(AtomicInteger usedTokens) {
        this.usedTokens = usedTokens;
        return this;
    }

    public AiParameters setModelName(String modelName) {
        this.modelName = modelName;
        return this;
    }

    public AiParameters setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    public AiParameters setResponseSchema(JsonObject responseSchema) {
        this.responseSchema = responseSchema;
        return this;
    }

    public String getPrompt() {
        return prompt;
    }

    public JsonArray getMessages() {
        if (messages == null && prompt != null) {
            messages = makeMessages(prompt);
        }
        return messages;
    }

    public double getTemperature() {
        return temperature;
    }

    public AtomicInteger getUsedTokens() {
        return usedTokens;
    }

    public String getModelName() {
        return modelName;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public JsonObject getResponseSchema() {
        return responseSchema;
    }

    private JsonArray makeMessages(String prompt) {
        JsonObject message = new JsonObject();
        message.put("role", "user");
        message.put("content", prompt);
        JsonArray messages = new JsonArray();
        messages.add(message);
        return messages;
    }
}
