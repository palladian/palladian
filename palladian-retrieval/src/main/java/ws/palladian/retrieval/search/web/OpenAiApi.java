package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.collection.MapBuilder;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A wrapper for OpenAI's APIs.
 *
 * @author David Urbansky
 * Created 15.02.2023
 */
public class OpenAiApi {
    private static final TimeWindowRequestThrottle THROTTLE = new TimeWindowRequestThrottle(1, TimeUnit.MINUTES, 3500);
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiApi.class);

    private final String apiKey;

    public static final String CONFIG_API_KEY = "api.openai.key";
    public static final String CONFIG_API_KEY_FALLBACK = "api.openai.apiKey";

    public OpenAiApi(String apiKey) {
        this.apiKey = apiKey;
    }

    public OpenAiApi(Configuration configuration) {
        this(Optional.ofNullable(configuration.getString(CONFIG_API_KEY)).orElse((configuration.getString(CONFIG_API_KEY_FALLBACK))));
    }

    public float[] getEmbedding(String text) throws Exception {
        return getEmbedding(text, null);
    }

    public float[] getEmbedding(String text, AtomicInteger usedTokens) throws Exception {
        DocumentRetriever documentRetriever = new DocumentRetriever();
        documentRetriever.setGlobalHeaders(MapBuilder.createPut("Content-Type", "application/json").put("Authorization", "Bearer " + apiKey).create());
        JsonObject requestJson = new JsonObject();
        requestJson.put("input", text);
        requestJson.put("model", "text-embedding-ada-002");
        THROTTLE.hold();
        String postResponseText = documentRetriever.tryPostJsonObject("https://api.openai.com/v1/embeddings", requestJson, false);
        JsonObject responseJson = JsonObject.tryParse(postResponseText);
        if (responseJson == null) {
            throw new Exception("Could not parse json " + postResponseText);
        }
        if (responseJson.tryQueryString("error/message") != null) {
            throw new Exception(responseJson.tryQueryString("error/message"));
        }

        JsonArray embeddingArray = responseJson.tryQueryJsonArray("data[0]/embedding");
        float[] embedding = new float[embeddingArray.size()];
        for (Object o : embeddingArray) {
            embedding[embeddingArray.indexOf(o)] = Float.parseFloat(o.toString());
        }

        if (usedTokens != null) {
            usedTokens.addAndGet(responseJson.tryQueryInt("usage/total_tokens"));
        }

        return embedding;
    }

    public String chat(String prompt) throws Exception {
        return chat(prompt, 1.0, null);
    }

    public String chat(String prompt, double temperature, AtomicInteger usedTokens) throws Exception {
        JsonObject message = new JsonObject();
        message.put("role", "user");
        message.put("content", prompt);
        JsonArray messages = new JsonArray();
        messages.add(message);

        return chat(messages, temperature, usedTokens);
    }

    public String chat(JsonArray messages, double temperature, AtomicInteger usedTokens) throws Exception {
        return chat(messages, temperature, usedTokens, "gpt-4");
    }

    public String chat(JsonArray messages, double temperature, AtomicInteger usedTokens, String modelName) throws Exception {
        DocumentRetriever documentRetriever = new DocumentRetriever();
        documentRetriever.setGlobalHeaders(MapBuilder.createPut("Content-Type", "application/json").put("Authorization", "Bearer " + apiKey).create());
        JsonObject requestJson = new JsonObject();
        requestJson.put("messages", messages);
        requestJson.put("model", modelName);
        requestJson.put("temperature", temperature);
        THROTTLE.hold();
        String postResponseText = documentRetriever.tryPostJsonObject("https://api.openai.com/v1/chat/completions", requestJson, false);
        JsonObject responseJson = JsonObject.tryParse(postResponseText);
        if (responseJson == null) {
            throw new Exception("Could not parse json " + postResponseText);
        }
        if (responseJson.tryQueryString("error/message") != null) {
            throw new Exception(responseJson.tryQueryString("error/message"));
        }

        String content = null;

        try {
            content = StringHelper.clean(responseJson.tryQueryString("choices[0]/message/content"));

            if (usedTokens != null) {
                usedTokens.addAndGet(responseJson.tryQueryInt("usage/total_tokens"));
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        return content;
    }

    public String completeText(String text) throws Exception {
        return completeText(text, "text-davinci-003", null);
    }

    public String completeText(String text, String modelName, AtomicInteger usedTokens) throws Exception {
        DocumentRetriever documentRetriever = new DocumentRetriever();
        documentRetriever.setGlobalHeaders(MapBuilder.createPut("Content-Type", "application/json").put("Authorization", "Bearer " + apiKey).create());
        JsonObject requestJson = new JsonObject();
        requestJson.put("prompt", text);
        requestJson.put("model", modelName);
        requestJson.put("temperature", 1.);
        requestJson.put("max_tokens", 64);
        requestJson.put("top_p", 1);
        THROTTLE.hold();
        String postResponseText = documentRetriever.tryPostJsonObject("https://api.openai.com/v1/completions", requestJson, false);
        JsonObject responseJson = JsonObject.tryParse(postResponseText);
        if (responseJson == null) {
            throw new Exception("Could not parse json " + postResponseText);
        }
        if (responseJson.tryQueryString("error/message") != null) {
            throw new Exception(responseJson.tryQueryString("error/message"));
        }

        String answer = responseJson.tryQueryString("choices[0]/text");

        if (usedTokens != null) {
            usedTokens.addAndGet(responseJson.tryQueryInt("usage/total_tokens"));
        }

        return StringHelper.clean(answer);
    }

    /**
     * @param prompt The textual prompt, e.g. "a black dog with a Christmas hat on"
     * @param size   The picture size, must be either '256x256', '512x512', or '1024x1024'
     */
    public String createImage(String prompt, String size) {
        DocumentRetriever documentRetriever = new DocumentRetriever();
        documentRetriever.setGlobalHeaders(MapBuilder.createPut("Authorization", "Bearer " + apiKey).create());

        JsonObject jsonObject = new JsonObject();
        jsonObject.put("prompt", prompt);
        jsonObject.put("n", 1);
        jsonObject.put("size", size);
        THROTTLE.hold();
        String responseText = documentRetriever.tryPostJsonObject("https://api.openai.com/v1/images/generations", jsonObject, false);
        System.out.println(responseText);

        JsonObject responseJson = JsonObject.tryParse(responseText);
        if (responseJson == null) {
            LOGGER.error("could not generate image " + responseText);
            return null;
        }
        JsonArray dataArray = responseJson.tryGetJsonArray("data");
        if (dataArray == null || dataArray.isEmpty()) {
            LOGGER.error("could not generate image " + responseText);
            return null;
        }
        return dataArray.tryGetJsonObject(0).tryGetString("url");
    }
}
