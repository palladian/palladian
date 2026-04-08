package ws.palladian.retrieval.search.web;

import org.junit.Ignore;
import org.junit.Test;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonObject;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore
public class GeminiApiTest {

    @Test
    public void testGeminiModels() {
        GeminiApi api = new GeminiApi("YOUR_API_KEY");

        api.setModel(GeminiApi.Model.GEMINI_3_0_PRO);
        assertEquals("gemini-3.0-pro", api.getModel());

        api.setModel(GeminiApi.Model.GEMINI_3_0_FLASH);
        assertEquals("gemini-3.0-flash", api.getModel());

        assertEquals("gemini-3.0-pro", GeminiApi.Model.GEMINI_3_0_PRO.toString());
        assertEquals("gemini-3.0-flash", GeminiApi.Model.GEMINI_3_0_FLASH.toString());
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
