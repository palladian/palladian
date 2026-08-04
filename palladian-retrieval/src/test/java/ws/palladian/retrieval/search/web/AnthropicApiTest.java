package ws.palladian.retrieval.search.web;

import org.junit.Test;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonObject;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Offline tests for {@link AnthropicApi}: request building (system-prompt hoisting and prompt-cache breakpoint) and
 * completion extraction (type-based, so a thinking block cannot swallow the answer). Uses the same subclass-and-stub
 * pattern as {@link GeminiApiTest} (override {@code executeRequest}) so no network call is made; here the stub also
 * captures the request JSON for assertions.
 *
 * @author David Urbansky
 */
public class AnthropicApiTest {

    private static class CapturingAnthropicApi extends AnthropicApi {
        JsonObject capturedRequest;
        /** Response the stub replies with; defaults to a single text block (what a non-thinking model returns). */
        String stubbedResponse = response(textBlock("ok"));

        CapturingAnthropicApi() {
            super("test-key");
        }

        @Override
        protected String executeRequest(String url, JsonObject request) {
            this.capturedRequest = request;
            return stubbedResponse;
        }
    }

    private static JsonObject textBlock(String text) {
        JsonObject block = new JsonObject();
        block.put("type", "text");
        block.put("text", text);
        return block;
    }

    /** A {@code thinking} block as {@code claude-sonnet-5} really sends it: a {@code type}, but no {@code text} key. */
    private static JsonObject thinkingBlock() {
        JsonObject block = new JsonObject();
        block.put("type", "thinking");
        block.put("thinking", "Let me consider the request…");
        block.put("signature", "abc123");
        return block;
    }

    /** Minimal valid Anthropic Messages response carrying the given content blocks. */
    private static String response(JsonObject... blocks) {
        return response("end_turn", blocks);
    }

    private static String response(String stopReason, JsonObject... blocks) {
        JsonObject response = new JsonObject();
        JsonArray content = new JsonArray();
        for (JsonObject block : blocks) {
            content.add(block);
        }
        response.put("content", content);
        response.put("stop_reason", stopReason);
        JsonObject usage = new JsonObject();
        usage.put("input_tokens", 10);
        usage.put("output_tokens", 5);
        response.put("usage", usage);
        return response.toString();
    }

    private static JsonObject message(String role, String contentText) {
        JsonObject message = new JsonObject();
        message.put("role", role);
        message.put("content", contentText);
        return message;
    }

    @Test
    public void systemMessageIsHoistedAndCached() throws Exception {
        CapturingAnthropicApi api = new CapturingAnthropicApi();
        JsonArray messages = new JsonArray();
        messages.add(message("system", "You are a helpful assistant."));
        messages.add(message("user", "Hi"));

        AtomicInteger usedTokens = new AtomicInteger(0);
        String response = api.chat(messages, 1.0, usedTokens, AnthropicApi.HAIKU_4_5, null, null);

        assertEquals("ok", response);
        assertEquals("token accounting still works", 15, usedTokens.get());

        JsonObject request = api.capturedRequest;

        // system hoisted to the top-level "system" field as a text content block
        JsonArray systemBlocks = request.getJsonArray("system");
        assertNotNull("system field should be present", systemBlocks);
        assertEquals(1, systemBlocks.size());
        JsonObject block = (JsonObject) systemBlocks.get(0);
        assertEquals("text", block.getString("type"));
        assertEquals("You are a helpful assistant.", block.getString("text"));

        // cache_control breakpoint on the system block
        JsonObject cacheControl = block.getJsonObject("cache_control");
        assertNotNull("cache_control should be set on the system block", cacheControl);
        assertEquals("ephemeral", cacheControl.getString("type"));

        // the system message must be removed from the messages array (Anthropic rejects a system role there)
        JsonArray sentMessages = request.getJsonArray("messages");
        assertEquals(1, sentMessages.size());
        assertEquals("user", ((JsonObject) sentMessages.get(0)).getString("role"));
    }

    @Test
    public void noSystemMessageMeansNoCacheControl() throws Exception {
        CapturingAnthropicApi api = new CapturingAnthropicApi();
        JsonArray messages = new JsonArray();
        messages.add(message("user", "Just a user message"));

        api.chat(messages, 1.0, null, AnthropicApi.HAIKU_4_5, null, null);

        JsonObject request = api.capturedRequest;
        // no stable prefix -> no system field and no cache_control (byte-identical to the old behavior)
        assertFalse("no system field when there is no system message", request.containsKey("system"));
        JsonArray sentMessages = request.getJsonArray("messages");
        assertEquals(1, sentMessages.size());
        assertFalse("a volatile user message must never be marked cacheable", ((JsonObject) sentMessages.get(0)).containsKey("cache_control"));
    }

    @Test
    public void onlyLastSystemBlockGetsCacheControl() throws Exception {
        CapturingAnthropicApi api = new CapturingAnthropicApi();
        JsonArray messages = new JsonArray();
        messages.add(message("system", "first system block"));
        messages.add(message("system", "second system block"));
        messages.add(message("user", "Hi"));

        api.chat(messages, 1.0, null, AnthropicApi.HAIKU_4_5, null, null);

        JsonArray systemBlocks = api.capturedRequest.getJsonArray("system");
        assertEquals(2, systemBlocks.size());
        assertFalse("only the last block is a breakpoint", ((JsonObject) systemBlocks.get(0)).containsKey("cache_control"));
        assertTrue("last block is the cache breakpoint", ((JsonObject) systemBlocks.get(1)).containsKey("cache_control"));
    }

    @Test
    public void convenienceOverloadBuildsSystemAndUser() throws Exception {
        CapturingAnthropicApi api = new CapturingAnthropicApi();

        api.chat("SYSTEM PROMPT", "USER PROMPT", AnthropicApi.HAIKU_4_5);

        JsonObject request = api.capturedRequest;
        JsonArray systemBlocks = request.getJsonArray("system");
        assertEquals("SYSTEM PROMPT", ((JsonObject) systemBlocks.get(0)).getString("text"));
        assertNotNull(((JsonObject) systemBlocks.get(0)).getJsonObject("cache_control"));

        JsonArray sentMessages = request.getJsonArray("messages");
        assertEquals(1, sentMessages.size());
        assertEquals("user", ((JsonObject) sentMessages.get(0)).getString("role"));
        assertEquals("USER PROMPT", ((JsonObject) sentMessages.get(0)).getString("content"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  completion extraction — by block TYPE, never by position
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    public void thinkingBlockFirst_answerStillExtracted() throws Exception {
        // the regression: claude-sonnet-5 returns [thinking, text] even without an extended-thinking request, so
        // content[0]/text was null and a paid-for completion was thrown away
        CapturingAnthropicApi api = new CapturingAnthropicApi();
        api.stubbedResponse = response(thinkingBlock(), textBlock("<p>The answer.</p>"));

        AtomicInteger usedTokens = new AtomicInteger(0);
        assertEquals("<p>The answer.</p>", api.chat(new JsonArray(), 1.0, usedTokens, AnthropicApi.SONNET_5, null, null));
        assertEquals("tokens are billed either way and must still be counted", 15, usedTokens.get());
    }

    @Test
    public void textOnlyResponse_unchanged() {
        // a non-thinking model's single-block response must come back byte-identical to the pre-fix behavior
        assertEquals("plain answer", AnthropicApi.extractText(JsonObject.tryParse(response(textBlock("plain answer")))));
    }

    @Test
    public void interleavedThinkingAndText_concatenatesEveryTextBlock() {
        String extracted = AnthropicApi.extractText(
                JsonObject.tryParse(response(thinkingBlock(), textBlock("first half"), thinkingBlock(), textBlock("second half"))));
        assertEquals("first half\nsecond half", extracted);
    }

    @Test
    public void noTextBlockAtAll_returnsNullWithoutThrowing() throws Exception {
        CapturingAnthropicApi api = new CapturingAnthropicApi();
        api.stubbedResponse = response(thinkingBlock());
        assertNull(api.chat(new JsonArray(), 1.0, null, AnthropicApi.SONNET_5, null, null));
    }

    @Test
    public void emptyOrMissingContent_returnsNull() {
        assertNull(AnthropicApi.extractText(JsonObject.tryParse(response())));
        assertNull(AnthropicApi.extractText(JsonObject.tryParse("{\"stop_reason\":\"end_turn\"}")));
        // a text block whose text is empty is not an answer either
        assertNull(AnthropicApi.extractText(JsonObject.tryParse(response(textBlock("")))));
    }

    @Test
    public void truncatedAnswer_isReturnedNotDiscarded() throws Exception {
        // stop_reason=max_tokens is a distinct condition: partial content, not an empty response
        CapturingAnthropicApi api = new CapturingAnthropicApi();
        api.stubbedResponse = response("max_tokens", thinkingBlock(), textBlock("<p>Half a sen"));
        assertEquals("<p>Half a sen", api.chat(new JsonArray(), 1.0, null, AnthropicApi.SONNET_5, null, null));
    }

    @Test
    public void describeContentBlockTypes_namesWhatTheModelSentInstead() {
        assertEquals("thinking, text", AnthropicApi.describeContentBlockTypes(JsonObject.tryParse(response(thinkingBlock(), textBlock("x")))));
        assertEquals("thinking", AnthropicApi.describeContentBlockTypes(JsonObject.tryParse(response(thinkingBlock()))));
        assertEquals("none", AnthropicApi.describeContentBlockTypes(JsonObject.tryParse(response())));
        assertEquals("none", AnthropicApi.describeContentBlockTypes(JsonObject.tryParse("{}")));
    }
}
