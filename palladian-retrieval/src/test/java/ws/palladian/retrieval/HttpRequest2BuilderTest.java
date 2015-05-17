package ws.palladian.retrieval;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class HttpRequest2BuilderTest {
    @Test
    public void testParseParams() {
        String url = "http://de.wikipedia.org/wiki/Spezial:Search?search=Katzen&go=Artikel";
        Map<String, String> params = HttpRequest2Builder.parseParams(url);
        assertEquals(2, params.size());
        assertEquals("Katzen", params.get("search"));
        assertEquals("Artikel", params.get("go"));
        // CollectionHelper.print(params);
    }

    @Test
    public void testCreateUrl() {
        String baseUrl = "http://de.wikipedia.org/wiki/Spezial:Search";
        Map<String, String> params = new HashMap<>();
        params.put("search", "Katzen");
        params.put("go", "Artikel");
        String fullUrl = HttpRequest2Builder.createFullUrl(baseUrl, params);
        assertEquals("http://de.wikipedia.org/wiki/Spezial:Search?search=Katzen&go=Artikel", fullUrl);
    }
}
