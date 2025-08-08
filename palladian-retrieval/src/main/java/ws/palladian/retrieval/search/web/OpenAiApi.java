package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.collection.IdTrie;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A wrapper for OpenAI's APIs.
 *
 * @author David Urbansky
 * Created 15.02.2023
 */
public class OpenAiApi extends AiApi {
    private static final TimeWindowRequestThrottle THROTTLE = new TimeWindowRequestThrottle(1, TimeUnit.MINUTES, 3500);
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiApi.class);

    private final String apiKey;
    private final String apiBase;
    private String model = DEFAULT_MODEL;
    private String serviceTier = null;
    private String verbosity = null;
    private Map<String, String> globalHeaders = new HashMap<>();
    private Map<String, String> queryParams = new HashMap<>();

    public static final String CONFIG_API_KEY = "api.openai.key";
    public static final String CONFIG_API_KEY_FALLBACK = "api.openai.apiKey";

    private static final String DEFAULT_MODEL = "gpt-4o";
    public static final String EMBEDDING_MODEL_SMALL = "text-embedding-3-small";
    public static final String EMBEDDING_MODEL_LARGE = "text-embedding-3-large";

    public static final String MODEL_GPT_3_5_TURBO = "gpt-3.5-turbo";
    public static final String MODEL_GPT_4_TURBO = "gpt-4-turbo";
    public static final String MODEL_GPT_4O = "gpt-4o";
    public static final String MODEL_GPT_4O_MINI = "gpt-4o-mini";
    public static final String MODEL_GPT_4O_NANO = "gpt-4o-nano";
    public static final String MODEL_GPT_5 = "gpt-5";
    public static final String MODEL_GPT_5_MINI = "gpt-5-mini";
    public static final String MODEL_GPT_5_NANO = "gpt-5-nano";

    public OpenAiApi(Configuration configuration) {
        this(Optional.ofNullable(configuration.getString(CONFIG_API_KEY)).orElse((configuration.getString(CONFIG_API_KEY_FALLBACK))));
    }

    public OpenAiApi(String apiKey) {
        this(apiKey, null);
    }

    public OpenAiApi(String model, String serviceTier, String apiKey) {
        this(apiKey, null, model, serviceTier);
    }

    public OpenAiApi(String apiKey, String apiBase) {
        this(apiKey, apiBase, DEFAULT_MODEL, null);
    }

    public OpenAiApi(String apiKey, String apiBase, String model, String serviceTier) {
        this.apiKey = apiKey;
        apiBase = Optional.ofNullable(apiBase).orElse("https://api.openai.com/v1");
        if (apiBase.endsWith("/")) {
            apiBase = apiBase.substring(0, apiBase.length() - 1);
        }
        this.apiBase = apiBase;
        this.globalHeaders.put("Authorization", "Bearer " + apiKey);
        this.model = model;
        this.serviceTier = serviceTier;
    }

    public float[] getEmbedding(String text) throws Exception {
        return getEmbedding(text, null);
    }

    public float[] getEmbedding(String text, AtomicInteger usedTokens) throws Exception {
        return getEmbedding(text, usedTokens, EMBEDDING_MODEL_SMALL);
    }

    public String getResponse(String text, String modelName, JsonArray tools) throws Exception {
        DocumentRetriever documentRetriever = new DocumentRetriever();
        HashMap<String, String> headers = new HashMap<>(globalHeaders);
        headers.put("Content-Type", "application/json");
        documentRetriever.setGlobalHeaders(headers);
        JsonObject requestJson = new JsonObject();
        requestJson.put("input", text);
        requestJson.put("model", modelName);
        if (!StringHelper.nullOrEmpty(tools)) {
            requestJson.put("tools", tools);
        }
        THROTTLE.hold();
        String postResponseText = documentRetriever.tryPostJsonObject(buildRequestUrl("/responses"), requestJson, false);
        JsonObject responseJson = JsonObject.tryParse(postResponseText);
        if (responseJson == null) {
            throw new Exception("Could not parse json " + postResponseText);
        }
        if (responseJson.tryQueryString("error/message") != null) {
            throw new Exception(responseJson.tryQueryString("error/message"));
        }

        String answer = (String) responseJson.queryJsonPath("$..output[?(@.type=='message')].content[0].text");

        return StringHelper.clean(answer);
    }

    public float[] getEmbedding(String text, AtomicInteger usedTokens, String embeddingModel) throws Exception {
        DocumentRetriever documentRetriever = new DocumentRetriever();
        HashMap<String, String> headers = new HashMap<>(globalHeaders);
        headers.put("Content-Type", "application/json");
        documentRetriever.setGlobalHeaders(headers);
        JsonObject requestJson = new JsonObject();
        requestJson.put("input", text);
        requestJson.put("model", embeddingModel);
        THROTTLE.hold();
        String postResponseText = documentRetriever.tryPostJsonObject(buildRequestUrl("/embeddings"), requestJson, false);
        JsonObject responseJson = JsonObject.tryParse(postResponseText);
        if (responseJson == null) {
            throw new Exception("Could not parse json " + postResponseText);
        }
        if (responseJson.tryQueryString("error/message") != null) {
            throw new Exception(responseJson.tryQueryString("error/message"));
        }

        JsonArray embeddingArray = responseJson.tryQueryJsonArray("data[0]/embedding");
        int vectorSize = embeddingArray.size();
        float[] embedding = new float[vectorSize];
        for (int i = 0; i < vectorSize; i++) {
            embedding[i] = Float.parseFloat(embeddingArray.tryGetString(i));
        }

        if (usedTokens != null) {
            usedTokens.addAndGet(responseJson.tryQueryInt("usage/total_tokens"));
        }

        return embedding;
    }

    public String chat(String systemPrompt, String userPrompt, String model) throws Exception {
        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.put("role", "system");
        message.put("content", systemPrompt);
        messages.add(message);
        message = new JsonObject();
        message.put("role", "user");
        message.put("content", userPrompt);
        messages.add(message);
        return chat(messages, 1., null, model, null, null);
    }

    @Override
    public String chat(JsonArray messages, double temperature, AtomicInteger usedTokens) throws Exception {
        return chat(messages, temperature, usedTokens, model, null, null);
    }

    public String chat(JsonArray messages) throws Exception {
        return chat(messages, 1., null, model, null, null);
    }

    public String chat(JsonArray messages, JsonObject jsonSchema) throws Exception {
        return chat(messages, 1., null, model, null, jsonSchema);
    }

    @Override
    public String chat(JsonArray messages, double temperature, AtomicInteger usedTokens, String modelName, Integer maxTokens, JsonObject jsonSchema) throws Exception {
        DocumentRetriever documentRetriever = new DocumentRetriever();
        HashMap<String, String> headers = new HashMap<>(globalHeaders);
        headers.put("Content-Type", "application/json");
        documentRetriever.setGlobalHeaders(headers);
        JsonObject requestJson = new JsonObject();
        requestJson.put("messages", messages);
        requestJson.put("model", modelName);
        requestJson.put("temperature", temperature);
        if (serviceTier != null) {
            requestJson.put("service_tier", serviceTier);
        }
        if (verbosity != null) {
            requestJson.put("verbosity", verbosity);
        }
        if (maxTokens != null) {
            requestJson.put("max_tokens", maxTokens);
        }
        if (jsonSchema != null) {
            JsonObject responseFormatJson = new JsonObject();
            responseFormatJson.put("type", "json_schema");
            responseFormatJson.put("json_schema", jsonSchema);
            requestJson.put("response_format", responseFormatJson);
        }
        THROTTLE.hold();
        String postResponseText = documentRetriever.postJsonObject(buildRequestUrl("/chat/completions"), requestJson, false);
        JsonObject responseJson = JsonObject.tryParse(postResponseText);
        if (responseJson == null) {
            throw new Exception("Could not parse json " + postResponseText);
        }
        if (responseJson.tryQueryString("error/message") != null) {
            throw new Exception(responseJson.tryQueryString("error/message"));
        }

        String content = null;

        try {
            content = responseJson.tryQueryString("choices[0]/message/content");

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
        HashMap<String, String> headers = new HashMap<>(globalHeaders);
        headers.put("Content-Type", "application/json");
        documentRetriever.setGlobalHeaders(headers);
        JsonObject requestJson = new JsonObject();
        requestJson.put("prompt", text);
        requestJson.put("model", modelName);
        requestJson.put("temperature", 1.);
        requestJson.put("max_tokens", 64);
        requestJson.put("top_p", 1);
        THROTTLE.hold();
        String postResponseText = documentRetriever.tryPostJsonObject(buildRequestUrl("/completions"), requestJson, false);
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

    public String createImage(String prompt, String size) {
        return createImage(prompt, size, null);
    }

    /**
     * @param prompt  The textual prompt, e.g. "a black dog with a Christmas hat on"
     * @param size    The picture size, see https://platform.openai.com/docs/pricing#image-generation
     * @param quality Either low, medium, or high
     */
    public String createImage(String prompt, String size, String quality) {
        DocumentRetriever documentRetriever = new DocumentRetriever();
        documentRetriever.getHttpRetriever().setSocketTimeout((int) TimeUnit.MINUTES.toMillis(5));
        documentRetriever.getHttpRetriever().setConnectionTimeout((int) TimeUnit.MINUTES.toMillis(5));
        documentRetriever.setGlobalHeaders(globalHeaders);

        JsonObject jsonObject = new JsonObject();
        jsonObject.put("prompt", prompt);
        jsonObject.put("n", 1);
        jsonObject.put("model", "gpt-image-1");
        if (quality != null) {
            jsonObject.put("quality", quality);
        }
        jsonObject.put("size", size);
        THROTTLE.hold();
        String responseText = documentRetriever.tryPostJsonObject(buildRequestUrl("/images/generations"), jsonObject, false);

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

        return dataArray.tryGetJsonObject(0).tryGetString("b64_json");
    }

    public static int estimateTokens(String text) {
        int tokens = 0;
        String[] words = text.split(" ");
        for (String word : words) {
            tokens += (int) Math.floor(word.length() / 3.);

            // every special char, e.g. attached ! or , counts as one token
            StringTokenizer stringTokenizer = new StringTokenizer(word, IdTrie.DELIMITERS);
            int c = 0;
            while (stringTokenizer.hasMoreTokens()) {
                stringTokenizer.nextToken();
                if (c > 0) {
                    tokens++;
                }
                c++;
            }
        }

        return tokens;
    }

    private String buildRequestUrl(String endpoint) {
        StringBuilder urlBuilder = new StringBuilder(apiBase).append(endpoint);
        String qs = urlBuilder.indexOf("?") == -1 ? "?" : "&";
        for (Map.Entry<String, String> stringStringEntry : this.queryParams.entrySet()) {
            urlBuilder.append(qs).append(stringStringEntry.getKey()).append("=").append(stringStringEntry.getValue());
            qs = "&";
        }
        return urlBuilder.toString();
    }

    public boolean addHeader(String key, String value) {
        return this.globalHeaders.put(key, value) != null;
    }

    public boolean removeHeader(String key) {
        return this.globalHeaders.remove(key) != null;
    }

    public boolean addQueryParam(String key, String value) {
        return this.queryParams.put(key, value) != null;
    }

    public boolean removeQueryParam(String key) {
        return this.queryParams.remove(key) != null;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    // this should be deprecated
    public void setDefaultModel(String model) {
        this.model = model;
    }

    public String getDefaultModel() {
        return DEFAULT_MODEL;
    }

    public String getServiceTier() {
        return serviceTier;
    }

    public void setServiceTier(String serviceTier) {
        this.serviceTier = serviceTier;
    }

    public String getVerbosity() {
        return verbosity;
    }

    public void setVerbosity(String verbosity) {
        this.verbosity = verbosity;
    }

    public static void main(String[] args) {
        System.out.println(OpenAiApi.estimateTokens("GPT-3.5 Turbo"));
        System.out.println(OpenAiApi.estimateTokens(
                "This state-of-the-art tool is designed to help users estimate their OpenAI API usage cost with utmost accuracy, providing a clear picture of what utilizing GPT-3.5 Turbo"));
        System.out.println(OpenAiApi.estimateTokens(
                "With the advance of artificial intelligence technologies comes the need for precise cost prediction tools. A prime example of this is the innovative and dynamic Free ChatGPT Token Calculator for the OpenAI API. This state-of-the-art tool is designed to help users estimate their OpenAI API usage cost with utmost accuracy, providing a clear picture of what utilizing GPT-3.5 Turbo or the upcoming GPT-4 Turbo will set you back financially. The token calculator essentially assists you in monitoring your expenditure by offering detailed analytics of the tokens used in API calls. Stay ahead in the tech world by deciphering the OpenAI API cost with the free ChatGPT GPT-3.5 and GPT-4 token pricing calculator and effectively leverage the power of GPT-3.5 and GPT-4 without worrying about unplanned expenses."));
    }
}
