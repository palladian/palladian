package ws.palladian.retrieval.search.web;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Offline tests for {@link OpenAiApi}'s model default.
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
}
