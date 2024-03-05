package ws.palladian.persistence.json;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import static org.hamcrest.CoreMatchers.is;

/**
 * @author David Urbansky
 * @since 18.02.2024 at 22:00
 **/
public class JsonUtilsTest {
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void testRewriteKeys() {
        JsonObject jso = new JsonObject();
        jso.put("testThis", "test");
        jso.put("another_key", JsonObject.tryParse("{\"camelCase\": \"test2\"}"));
        JsonUtils.snakeCaseKeys(jso);

        collector.checkThat(jso.get("test_this"), is("test"));
        collector.checkThat(jso.tryQueryString("another_key/camel_case"), is("test2"));
    }
}