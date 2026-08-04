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
