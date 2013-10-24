package ws.palladian.retrieval.parser.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * <p>
 * Test JSON parsing.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public class JsonParserTest {
    private final String jsonString = "{'entry': {'a': 1,'b':['1a',['one','two'],{'f':1.48,'h': 2.22}],'c': {'d':'2b'}, 'd': null}}";

    @Test
    public void testGet() throws JsonException {

        JsonObject jsonObject = new JsonObject(jsonString);
        assertEquals(jsonObject.getJsonObject("entry"), jsonObject.query("entry"));
        assertEquals(Integer.valueOf(1), jsonObject.query("entry/a"));
        assertEquals(Integer.valueOf(1), jsonObject.query("/entry/a"));
        assertEquals("2b", jsonObject.query("entry/c/d"));
        assertEquals("1a", jsonObject.query("entry/b[0]"));
        assertEquals("two", jsonObject.query("entry/b[1][1]"));
        assertEquals(1.48, (Double)jsonObject.query("entry/b[2]/f"), 0.001);
        assertEquals(2.22, (Double)jsonObject.query("entry/b[2]/h"), 0.001);

        assertNull(jsonObject.tryGetBoolean("entry"));
        JsonArray jsonArray = jsonObject.queryJsonArray("/entry/b");
        assertNotNull(jsonArray);
        assertNull(jsonArray.tryGetString(3));

        assertNull(jsonObject.query("/entry/d"));
        assertNull(jsonObject.getJsonObject("entry").getString("d"));
    }

    @Test
    public void testGetNonExisting() throws JsonException {
        JsonObject jsonObject = new JsonObject(jsonString);
        try {
            jsonObject.query("entry/b[3]/g");
            fail();
        } catch (JsonException e) {
            assertEquals("Illegal index: 3", e.getMessage());
        }
        try {
            jsonObject.query("entry/b[2]/g");
            fail();
        } catch (JsonException e) {
            assertEquals("No key: g", e.getMessage());
        }
        try {
            jsonObject.query("entry1/b[3]/g");
            fail();
        } catch (JsonException e) {
            assertEquals("No key: entry1", e.getMessage());
        }
        try {
            jsonObject.getString("test");
            fail();
        } catch (JsonException e) {
            assertEquals("No key: test", e.getMessage());
        }
    }

    @Test
    public void testGetWrongType() throws JsonException {
        JsonObject jsonObject = new JsonObject(jsonString);
        try {
            jsonObject.getJsonObject("entry").getLong("d");
        } catch (JsonException e) {
            assertEquals("Could not parse \"null\" to long.", e.getMessage());
        }
    }


}