package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.collection.MapBuilder;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A wrapper for Anthropic's APIs.
 *
 * @author David Urbansky
 * Created 20.03.2024
 * See https://docs.anthropic.com/claude/reference
 */
public class AnthropicApi extends AiApi {
    private static final TimeWindowRequestThrottle THROTTLE = new TimeWindowRequestThrottle(1, TimeUnit.MINUTES, 5);
    private static final Logger LOGGER = LoggerFactory.getLogger(AnthropicApi.class);

    private final String apiKey;

    public static final String CONFIG_API_KEY = "api.anthropic.key";

    /** See https://docs.anthropic.com/claude/docs/models-overview */
    private static final String DEFAULT_MODEL = "claude-3-5-sonnet-20240620";

    public AnthropicApi(String apiKey) {
        this.apiKey = apiKey;
    }

    public AnthropicApi(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    @Override
    public String chat(JsonArray messages, double temperature, AtomicInteger usedTokens) throws Exception {
        return chat(messages, temperature, usedTokens, DEFAULT_MODEL, null);
    }

    @Override
    public String chat(JsonArray messages, double temperature, AtomicInteger usedTokens, String modelName, Integer maxTokens) throws Exception {
        DocumentRetriever documentRetriever = new DocumentRetriever();
        documentRetriever.setGlobalHeaders(MapBuilder.createPut("Content-Type", "application/json").put("x-api-key", apiKey).put("anthropic-version", "2023-06-01").create());
        JsonObject requestJson = new JsonObject();
        requestJson.put("messages", messages);
        requestJson.put("model", modelName);
        requestJson.put("temperature", temperature);
        requestJson.put("max_tokens", Optional.ofNullable(maxTokens).orElse(4096));

        THROTTLE.hold();
        String postResponseText = documentRetriever.postJsonObject("https://api.anthropic.com/v1/messages", requestJson, false);
        JsonObject responseJson = JsonObject.tryParse(postResponseText);
        if (responseJson == null) {
            throw new Exception("Could not parse json " + postResponseText);
        }
        if (responseJson.tryQueryString("error/message") != null) {
            throw new Exception(responseJson.tryQueryString("error/message"));
        }

        String content = null;

        try {
            content = responseJson.tryQueryString("content[0]/text");

            if (usedTokens != null) {
                usedTokens.addAndGet(responseJson.tryQueryInt("usage/input_tokens") + responseJson.tryQueryInt("usage/output_tokens"));
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        return content;
    }

    public static void main(String[] args) throws Exception {
        AnthropicApi anthropicApi = new AnthropicApi("YOUR_API_KEY");
        String chat = anthropicApi.chat("What is the meaning of life?");
        System.out.println(chat);
    }
}
