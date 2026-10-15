package ws.palladian.retrieval.search.web;

import org.junit.Test;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonObject;

import static org.junit.Assert.*;

/**
 * Offline tests for {@link OpenAiApi}'s model default and request body.
 *
 * @author David Urbansky
 */
public class OpenAiApiTest {

    /** OpenAI retires gpt-4o (the old default), gpt-4o-mini, gpt-4.1 and gpt-4.1-mini on 2027-01-14; a call that names no model must keep working. */
    @Test
    public void aCallThatNamesNoModelGetsOneThatIsNotRetiring() {
        OpenAiApi api = new OpenAiApi("key");
        assertEquals(OpenAiApi.MODEL_GPT_5_6_LUNA, api.getModel());
        assertEquals(api.getModel(), api.getDefaultModel());
    }

    @Test
    public void gpt5AndLaterAreReasoningModels() {
        assertTrue(OpenAiApi.isReasoningModel("gpt-5-mini"));
        assertTrue(OpenAiApi.isReasoningModel("gpt-5.6-luna"));
        assertTrue(OpenAiApi.isReasoningModel("gpt-6-luna"));
        assertTrue(OpenAiApi.isReasoningModel("gpt-6.1-sol"));
        assertTrue(OpenAiApi.isReasoningModel("gpt-10"));
        assertFalse(OpenAiApi.isReasoningModel("gpt-4o-mini"));
        assertFalse(OpenAiApi.isReasoningModel("gpt-4.1"));
        assertFalse(OpenAiApi.isReasoningModel("gpt-3.5-turbo"));
    }

    /** gpt-6 answers only with max_completion_tokens and without temperature, and must get the effort it was asked for. */
    @Test
    public void aGpt6RequestIsAReasoningRequest() {
        OpenAiApi api = new OpenAiApi("key");
        JsonObject body = api.chatRequest(new JsonArray(), 0.7, "gpt-6-luna", 1200, null, "none");
        assertEquals("none", body.tryGetString("reasoning_effort"));
        assertEquals(1200, (int) body.tryGetInt("max_completion_tokens"));
        assertFalse(body.containsKey("max_tokens"));
        assertFalse(body.containsKey("temperature"));
    }

    @Test
    public void anOlderModelKeepsTemperatureAndMaxTokens() {
        OpenAiApi api = new OpenAiApi("key");
        JsonObject body = api.chatRequest(new JsonArray(), 0.7, "gpt-4o-mini", 600, null, "none");
        assertEquals(0.7, body.tryGetDouble("temperature"), 0.0001);
        assertEquals(600, (int) body.tryGetInt("max_tokens"));
        assertFalse(body.containsKey("reasoning_effort"));
        assertFalse(body.containsKey("max_completion_tokens"));
    }
}
