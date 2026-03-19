package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.collection.MapBuilder;
import ws.palladian.helper.nlp.PatternHelper;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * A wrapper for MiniMax APIs.
 *
 * See https://platform.minimax.io/docs/api-reference/api-overview
 */
public class MiniMaxApi extends AiApi {
    private static final TimeWindowRequestThrottle THROTTLE = new TimeWindowRequestThrottle(1, TimeUnit.MINUTES, 5);
    private static final Logger LOGGER = LoggerFactory.getLogger(MiniMaxApi.class);

    private final String apiKey;

    public static final String CONFIG_API_KEY = "api.minimax.key";

    public static final String M2_5 = "MiniMax-M2.5";
    public static final String M2_7 = "MiniMax-M2.7";
    public static final String MINIMAX_TEXT_01 = "MiniMax-Text-01";
    public static final String ABAB_6_5S_CHAT = "abab6.5s-chat";
    public static final String ABAB_6_5_CHAT = "abab6.5-chat";
    public static final String ABAB_6_CHAT = "abab6-chat";
    private static final String DEFAULT_MODEL = M2_5;

    private String model = DEFAULT_MODEL;

    public MiniMaxApi(String apiKey) {
        this.apiKey = apiKey;
    }

    public MiniMaxApi(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public String chat(JsonArray messages, double temperature, AtomicInteger usedTokens) throws Exception {
        return chat(messages, temperature, usedTokens, model, null, null);
    }

    @Override
    public String chat(JsonArray messages, double temperature, AtomicInteger usedTokens, String modelName, Integer maxTokens, JsonObject responseSchema) throws Exception {
        DocumentRetriever documentRetriever = new DocumentRetriever();
        documentRetriever.setGlobalHeaders(MapBuilder.createPut("Content-Type", "application/json").put("Authorization", "Bearer " + apiKey).create());
        JsonObject requestJson = new JsonObject();
        requestJson.put("messages", messages);
        requestJson.put("model", modelName);
        requestJson.put("temperature", temperature);
        if (maxTokens != null) {
            requestJson.put("max_tokens", maxTokens);
        }

        THROTTLE.hold();
        String postResponseText = documentRetriever.postJsonObject("https://api.minimaxi.chat/v1/text/chatcompletion_v2", requestJson, false);
        JsonObject responseJson = JsonObject.tryParse(postResponseText);
        if (responseJson == null) {
            throw new Exception("Could not parse json " + postResponseText);
        }
        if (responseJson.tryQueryString("base_resp/status_msg") != null && !responseJson.tryQueryString("base_resp/status_msg").isEmpty()) {
            throw new Exception(responseJson.tryQueryString("base_resp/status_msg"));
        }

        String content = null;

        try {
            content = responseJson.tryQueryString("choices[0]/message/content");

            if (content == null) {
                content = "";
            }

            // remove <think>...</think> tags and content
            if (content != null) {
                content = PatternHelper.compileOrGet("<think>.*?</think>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(content).replaceAll("").trim();
            }

            if (usedTokens != null) {
                usedTokens.addAndGet(responseJson.tryQueryInt("usage/total_tokens"));
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        return content;
    }

    public static void main(String[] args) throws Exception {
        MiniMaxApi api = new MiniMaxApi("YOUR_API_KEY");
        String chat = api.chat("What is the meaning of life?");
        System.out.println(chat);
    }
}