package ws.palladian.retrieval.search.web;

import org.junit.Test;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonObject;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Offline tests for {@link GeminiApi}. Every test stubs {@code executeRequest} or calls a pure helper, so nothing here
 * touches the network — the class used to be {@code @Ignore}d wholesale, which meant the answer-extraction path had no
 * coverage at all.
 */
public class GeminiApiTest {

    /**
     * {@code setModel} must pass the enum's API string through verbatim — the constant name and the wire id differ
     * (and the wire id is what the provider validates), so the enum is the single source of truth here.
     */
    @Test
    public void testGeminiModels() {
        GeminiApi api = new GeminiApi("YOUR_API_KEY");

        api.setModel(GeminiApi.Model.GEMINI_3_0_PRO);
        assertEquals("gemini-3-pro-preview", api.getModel());

        api.setModel(GeminiApi.Model.GEMINI_3_5_FLASH);
        assertEquals("gemini-3.5-flash", api.getModel());

        assertEquals("gemini-3-flash-preview", GeminiApi.Model.GEMINI_3_0_FLASH.toString());
        assertEquals("gemini-3.1-flash-lite", GeminiApi.Model.GEMINI_3_1_FLASH_LITE.toString());
    }

    @Test
    public void testChatSaxony() throws Exception {
        GeminiApi api = new MockGeminiApi();

        JsonArray messages = new JsonArray();
        JsonObject msg = new JsonObject();
        msg.put("role", "user");
        msg.put("content", "What is the capital of Saxony?");
        messages.add(msg);

        String response = api.chat(messages, 1.0, new AtomicInteger(0));
        assertTrue("Response should contain Dresden", response.contains("Dresden"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  answer extraction — skips reasoning parts, never positional
    // ═══════════════════════════════════════════════════════════════════════

    /** {@code parts[0]} is the answer only for a non-reasoning model — this must keep working unchanged. */
    @Test
    public void singleTextPart_isTheAnswer() {
        assertEquals("The answer.", GeminiApi.extractText(content(textPart("The answer."))));
    }

    @Test
    public void thoughtPartFirst_answerStillExtracted() {
        // a thinking model puts its reasoning first; reading parts[0].text would return the reasoning or null
        assertEquals("The answer.", GeminiApi.extractText(content(thoughtPart("Considering the options…"), textPart("The answer."))));
    }

    @Test
    public void textlessPartFirst_isSkipped() {
        // Gemini 3 can emit a part carrying only a thoughtSignature, with no text key at all
        JsonObject signatureOnly = new JsonObject();
        signatureOnly.put("thoughtSignature", "abc123");
        assertEquals("The answer.", GeminiApi.extractText(content(signatureOnly, textPart("The answer."))));
    }

    @Test
    public void multipleAnswerParts_areConcatenated() {
        assertEquals("first\nsecond", GeminiApi.extractText(content(textPart("first"), textPart("second"))));
    }

    @Test
    public void noAnswerPart_returnsNull() {
        assertNull(GeminiApi.extractText(content(thoughtPart("only thinking"))));
        assertNull(GeminiApi.extractText(content()));
        assertNull("no parts key at all", GeminiApi.extractText(new JsonObject()));
        assertNull(GeminiApi.extractText(null));
    }

    private static JsonObject textPart(String text) {
        JsonObject part = new JsonObject();
        part.put("text", text);
        return part;
    }

    private static JsonObject thoughtPart(String text) {
        JsonObject part = textPart(text);
        part.put("thought", true);
        return part;
    }

    private static JsonObject content(JsonObject... parts) {
        JsonArray partsArray = new JsonArray();
        for (JsonObject part : parts) {
            partsArray.add(part);
        }
        JsonObject content = new JsonObject();
        content.put("parts", partsArray);
        return content;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  reasoning controls — omitted unless asked for
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Neither knob set must omit {@code thinkingConfig} altogether rather than send an empty object — every existing
     * caller goes through this path, and an empty config is a request shape the provider has never been sent.
     */
    @Test
    public void thinkingConfig_omittedWhenUnset() {
        assertNull(GeminiApi.buildThinkingConfig(null, null));
    }

    @Test
    public void thinkingConfig_levelOnly() {
        JsonObject config = GeminiApi.buildThinkingConfig("low", null);
        assertEquals("low", config.tryGetString("thinkingLevel"));
        assertNull("budget must not be invented when only a level was set", config.tryGetInt("thinkingBudget"));
    }

    @Test
    public void thinkingConfig_budgetOnly() {
        JsonObject config = GeminiApi.buildThinkingConfig(null, Integer.valueOf(0));
        assertEquals(Integer.valueOf(0), config.tryGetInt("thinkingBudget"));
        assertNull("level must not be invented when only a budget was set", config.tryGetString("thinkingLevel"));
    }

    /** Both are accepted together so a caller can straddle model generations without a code change. */
    @Test
    public void thinkingConfig_bothKnobs() {
        JsonObject config = GeminiApi.buildThinkingConfig("high", Integer.valueOf(2048));
        assertEquals("high", config.tryGetString("thinkingLevel"));
        assertEquals(Integer.valueOf(2048), config.tryGetInt("thinkingBudget"));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  generationConfig — what actually goes on the wire
    // ════════════════════════════════════════════════════════════════════════

    /**
     * The legacy file overload must keep sending temperature 1.0 and nothing else — callers other than the recipe
     * extractor still use it, and silently constraining them would be an untested behaviour change.
     */
    @Test
    public void legacyFileOverload_sendsTemperatureOneAndNoConstraints() throws Exception {
        CapturingGeminiApi api = new CapturingGeminiApi();
        api.chat("prompt", "some-model");

        JsonObject generationConfig = api.lastRequest.getJsonObject("generationConfig");
        assertEquals(1.0, generationConfig.getDouble("temperature"), 0.0001);
        assertNull(generationConfig.tryGetInt("maxOutputTokens"));
        assertNull(generationConfig.tryGetJsonObject("response_schema"));
        assertNull("unset reasoning controls must not reach the wire", generationConfig.tryGetJsonObject("thinkingConfig"));
    }

    /**
     * The constrained overload is the whole point of this change: it must carry the low temperature, the output cap,
     * the schema (with the JSON mime type that activates it) and the reasoning control in one request.
     */
    @Test
    public void constrainedOverload_sendsEveryConstraint() throws Exception {
        CapturingGeminiApi api = new CapturingGeminiApi();
        api.setThinkingLevel("low");

        JsonObject schema = new JsonObject();
        schema.put("type", "OBJECT");

        api.chat("prompt", 0.2, "some-model", Integer.valueOf(8192), schema);

        JsonObject generationConfig = api.lastRequest.getJsonObject("generationConfig");
        assertEquals(0.2, generationConfig.getDouble("temperature"), 0.0001);
        assertEquals(Integer.valueOf(8192), generationConfig.tryGetInt("maxOutputTokens"));
        assertEquals("application/json", generationConfig.tryGetString("response_mime_type"));
        assertEquals("OBJECT", generationConfig.getJsonObject("response_schema").tryGetString("type"));
        assertEquals("low", generationConfig.getJsonObject("thinkingConfig").tryGetString("thinkingLevel"));
    }

    /** Records the outgoing request so the wire shape can be asserted without a network call. */
    private static class CapturingGeminiApi extends GeminiApi {
        private JsonObject lastRequest;

        public CapturingGeminiApi() {
            super("YOUR_API_KEY");
        }

        @Override
        protected String executeRequest(String url, JsonObject request) {
            this.lastRequest = request;
            JsonObject part = new JsonObject();
            part.put("text", "{}");
            JsonArray parts = new JsonArray();
            parts.add(part);
            JsonObject content = new JsonObject();
            content.put("parts", parts);
            JsonObject candidate = new JsonObject();
            candidate.put("content", content);
            JsonArray candidates = new JsonArray();
            candidates.add(candidate);
            JsonObject response = new JsonObject();
            response.put("candidates", candidates);
            return response.toString();
        }
    }

    private static class MockGeminiApi extends GeminiApi {
        public MockGeminiApi() {
            super("YOUR_API_KEY");
        }

        @Override
        protected String executeRequest(String url, JsonObject request) {
            // Construct a valid Gemini response JSON structure
            JsonObject response = new JsonObject();
            JsonArray candidates = new JsonArray();
            JsonObject candidate = new JsonObject();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.put("text", "The capital of Saxony is Dresden.");
            parts.add(part);
            content.put("parts", parts);
            candidate.put("content", content);
            candidates.add(candidate);
            response.put("candidates", candidates);

            return response.toString();
        }
    }
}
