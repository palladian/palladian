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

    // See https://docs.anthropic.com/claude/docs/models-overview
    public static final String HAIKU_4_5 = "claude-haiku-4-5";
    public static final String SONNET_4_5 = "claude-sonnet-4-5";
    public static final String SONNET_4_6 = "claude-sonnet-4-6";
    public static final String SONNET_5 = "claude-sonnet-5";
    public static final String OPUS_4_6 = "claude-opus-4-6";
    private static final String DEFAULT_MODEL = HAIKU_4_5;

    private String model = DEFAULT_MODEL;

    public AnthropicApi(String apiKey) {
        this.apiKey = apiKey;
    }

    public AnthropicApi(Configuration configuration) {
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
        JsonObject requestJson = buildRequestJson(messages, temperature, modelName, maxTokens);

        String postResponseText = executeRequest("https://api.anthropic.com/v1/messages", requestJson);
        JsonObject responseJson = JsonObject.tryParse(postResponseText);
        if (responseJson == null) {
            throw new Exception("Could not parse json " + postResponseText);
        }
        if (responseJson.tryQueryString("error/message") != null) {
            throw new Exception(responseJson.tryQueryString("error/message"));
        }

        // account for the tokens first: they are billed no matter whether we manage to read the completion below
        if (usedTokens != null) {
            try {
                usedTokens.addAndGet(responseJson.tryQueryInt("usage/input_tokens") + responseJson.tryQueryInt("usage/output_tokens"));
            } catch (Exception e) {
                LOGGER.warn("Could not read the token usage of the " + modelName + " response: " + e.getMessage());
            }
        }

        String content = extractText(responseJson);
        String stopReason = responseJson.tryGetString("stop_reason");

        // a truncated completion must never look like an empty one — the caller's remedy is a higher max_tokens, not a
        // retry with another model
        if ("max_tokens".equals(stopReason)) {
            LOGGER.warn(modelName + " hit its max_tokens limit, the completion is truncated (" + (content == null ? "no text at all" : content.length() + " chars") + ")");
        }
        if (content == null) {
            LOGGER.warn("The " + modelName + " response carried no text content block (stop_reason=" + stopReason + ", content blocks: " + describeContentBlockTypes(responseJson)
                    + ")");
        }

        return content;
    }

    /**
     * Extracts the completion from an Anthropic Messages response by <b>content block type</b>, never by position.
     * <p>
     * {@code content} is an array of typed blocks, and a thinking-capable model puts a {@code thinking} block first —
     * {@code claude-sonnet-5} does so even when the request never asks for extended thinking, answering in
     * {@code content[1]}. Reading {@code content[0]/text} therefore returned {@code null} for a perfectly good
     * completion while the tokens were still billed (this silently emptied every generated "Games Like X" SEO
     * description for 18 days). Same trap for any future {@code tool_use}/{@code redacted_thinking} block.
     * <p>
     * All {@code text} blocks are concatenated (newline-separated) rather than just the first, so an interleaved
     * thinking/text/thinking/text response keeps every part of the answer. A single-{@code text}-block response — what
     * every non-thinking model returns — comes back byte-identical to the previous behavior.
     *
     * @return the completion text, or {@code null} if the response contains no non-empty text block.
     */
    static String extractText(JsonObject responseJson) {
        JsonArray content = responseJson.tryGetJsonArray("content");
        if (content == null) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < content.size(); i++) {
            JsonObject block = content.tryGetJsonObject(i);
            if (block == null || !"text".equals(block.tryGetString("type"))) {
                continue;
            }
            String blockText = block.tryGetString("text");
            if (blockText == null || blockText.isEmpty()) {
                continue;
            }
            if (text.length() > 0) {
                text.append("\n");
            }
            text.append(blockText);
        }
        return text.length() == 0 ? null : text.toString();
    }

    /**
     * Lists the {@code type} of every content block, so a "no text block" warning names what the model sent instead
     * (e.g. {@code thinking} only) and a new block type is diagnosable from the log alone.
     */
    static String describeContentBlockTypes(JsonObject responseJson) {
        JsonArray content = responseJson.tryGetJsonArray("content");
        if (content == null || content.isEmpty()) {
            return "none";
        }
        StringBuilder types = new StringBuilder();
        for (int i = 0; i < content.size(); i++) {
            JsonObject block = content.tryGetJsonObject(i);
            if (types.length() > 0) {
                types.append(", ");
            }
            types.append(block == null ? "?" : String.valueOf(block.tryGetString("type")));
        }
        return types.toString();
    }

    /**
     * Convenience overload mirroring {@link OpenAiApi#chat(String, String, String)}: pass the stable instructions as a
     * dedicated system prompt. The system prompt is hoisted into Anthropic's top-level {@code system} field and marked
     * cacheable (see {@link #buildRequestJson}), so a large, stable system prompt is served from Anthropic's prompt
     * cache on repeated calls at ~0.1x input cost.
     */
    public String chat(String systemPrompt, String userPrompt, String modelName) throws Exception {
        JsonArray messages = new JsonArray();
        JsonObject systemMessage = new JsonObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);
        JsonObject userMessage = new JsonObject();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);
        return chat(messages, 1., null, modelName, null, null);
    }

    /**
     * Build the Anthropic request body from OpenAI-style messages.
     * <p>
     * Anthropic's Messages API only accepts {@code user}/{@code assistant} roles inside {@code messages} and expects the
     * system prompt as a separate top-level {@code system} field. We therefore hoist any {@code system}-role messages
     * into that field (as {@code text} content blocks) and attach a {@code cache_control} breakpoint to the last system
     * block. Because prompt caching is a prefix match, this lets a large, stable system prefix be served from cache on
     * repeated requests. Prompt caching is GA on {@code anthropic-version: 2023-06-01}, so no beta header is required.
     * <p>
     * When there is no system message the request is byte-for-byte identical to the previous behavior (no {@code system}
     * field, no {@code cache_control}) — a volatile user prompt is never marked cacheable, so no cache-write premium is
     * paid for nothing.
     */
    JsonObject buildRequestJson(JsonArray messages, double temperature, String modelName, Integer maxTokens) {
        JsonArray chatMessages = new JsonArray();
        JsonArray systemBlocks = new JsonArray();
        for (int i = 0; i < messages.size(); i++) {
            JsonObject message = (JsonObject) messages.get(i);
            if ("system".equals(message.tryGetString("role"))) {
                JsonObject systemBlock = new JsonObject();
                systemBlock.put("type", "text");
                systemBlock.put("text", message.tryGetString("content"));
                systemBlocks.add(systemBlock);
            } else {
                chatMessages.add(message);
            }
        }

        JsonObject requestJson = new JsonObject();
        if (!systemBlocks.isEmpty()) {
            JsonObject cacheControl = new JsonObject();
            cacheControl.put("type", "ephemeral");
            ((JsonObject) systemBlocks.get(systemBlocks.size() - 1)).put("cache_control", cacheControl);
            requestJson.put("system", systemBlocks);
        }
        requestJson.put("messages", chatMessages);
        requestJson.put("model", modelName);
        requestJson.put("temperature", temperature);
        requestJson.put("max_tokens", Optional.ofNullable(maxTokens).orElse(4096));
        return requestJson;
    }

    /**
     * Perform the HTTP POST. Isolated so tests can subclass and stub the network call (see {@code AnthropicApiTest},
     * same pattern as {@link GeminiApi}).
     */
    protected String executeRequest(String url, JsonObject request) throws Exception {
        DocumentRetriever documentRetriever = new DocumentRetriever();
        documentRetriever.setGlobalHeaders(MapBuilder.createPut("Content-Type", "application/json").put("x-api-key", apiKey).put("anthropic-version", "2023-06-01").create());
        THROTTLE.hold();
        return documentRetriever.postJsonObject(url, request, false);
    }

    public static void main(String[] args) throws Exception {
        AnthropicApi anthropicApi = new AnthropicApi("YOUR_API_KEY");
        String chat = anthropicApi.chat("What is the meaning of life?");
        System.out.println(chat);
    }
}
