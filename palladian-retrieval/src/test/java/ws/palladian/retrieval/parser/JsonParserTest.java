package ws.palladian.retrieval.parser;

import static org.junit.Assert.assertEquals;

import org.json.JSONException;
import org.junit.Test;

import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * <p>
 * Test Json parsing.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public class JsonParserTest {

    @Test
    public void testGet() throws JSONException {
        String jsonString = "{'entry': {'a': 1,'b':['1a',['one','two'],{'f':1.48,'h': 2.22}],'c': {'d':'2b'}}}";

        JsonObject jsonObject = new JsonObject(jsonString);
        assertEquals(jsonObject.getJsonObject("entry"), jsonObject.query("entry"));
        assertEquals(Integer.valueOf(1), jsonObject.query("entry/a"));
        assertEquals(Integer.valueOf(1), jsonObject.query("/entry/a"));
        assertEquals("2b", jsonObject.query("entry/c/d"));
        assertEquals("1a", jsonObject.query("entry/b[0]"));
        assertEquals("two", jsonObject.query("entry/b[1][1]"));
        assertEquals(1.48, (Double)jsonObject.query("entry/b[2]/f"), 0.001);
        assertEquals(2.22, (Double)jsonObject.query("entry/b[2]/h"), 0.001);
        assertEquals(null, jsonObject.query("entry/b[2]/g"));
        assertEquals(null, jsonObject.query("entry/b[3]/g"));
        assertEquals(null, jsonObject.query("entry1/b[3]/g"));
    }

}