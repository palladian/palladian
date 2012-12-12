package ws.palladian.helper.json;

import static org.junit.Assert.assertEquals;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import ws.palladian.helper.html.JPathHelper;

/**
 * <p>
 * Test cases for {@link JPathHelper}.
 * </p>
 * 
 * @author David Urbansky
 */
public class JPathHelperTest {

    /**
     * <p>
     * Test jPath with the following object.
     * </p>
     * 
     * <pre>
     * {
     *   'entry': {
     *     'a': 1,
     *     'b': [
     *       '1a',
     *       [
     *         'one',
     *         'two'
     *       ],
     *       {
     *         'f': 1.48
     *       }
     *     ],
     *     'c': {
     *       'd': '2b'
     *     }
     *   }
     * }
     * </pre>
     * 
     * @throws JSONException
     */
    @Test
    public void testGet() throws JSONException {
        String jsonString = "{'entry': {'a': 1,'b':['1a',['one','two'],{'f':1.48}],'c': {'d':'2b'}}}";

        JSONObject json = new JSONObject(jsonString);

        // System.out.println(JPathHelper.get(json, "entry", JSONObject.class));
        // System.out.println(JPathHelper.get(json, "entry/a", Integer.class));
        // System.out.println(JPathHelper.get(json, "entry/c/d", String.class));
        // System.out.println(JPathHelper.get(json, "entry/b[0]", String.class));
        // System.out.println(JPathHelper.get(json, "entry/b[1][1]", String.class));
        // System.out.println(JPathHelper.get(json, "entry/b[2]/f", Double.class));

        assertEquals(json.getJSONObject("entry"), JPathHelper.get(json, "entry", JSONObject.class));
        assertEquals(1L, Long.valueOf(JPathHelper.get(json, "entry/a", Integer.class)).longValue());
        assertEquals("2b", JPathHelper.get(json, "entry/c/d", String.class));
        assertEquals("1a", JPathHelper.get(json, "entry/b[0]", String.class));
        assertEquals("two", JPathHelper.get(json, "entry/b[1][1]", String.class));
        assertEquals(1.48, JPathHelper.get(json, "entry/b[2]/f", Double.class), 0.001);
    }

}
