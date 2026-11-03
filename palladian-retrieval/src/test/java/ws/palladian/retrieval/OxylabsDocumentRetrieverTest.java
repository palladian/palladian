package ws.palladian.retrieval;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** Which Oxylabs answers count as a page. Offline: the results are built, nothing is sent. */
public class OxylabsDocumentRetrieverTest {
    private static final String URL = "https://www.imdb.com/title/tt36586020";

    private static HttpResult result(int status, String body) {
        return new HttpResult(URL, body.getBytes(StandardCharsets.UTF_8), Collections.emptyMap(), status, body.length());
    }

    /**
     * On 2026-11-03 Oxylabs rendered imdb.com's real title page (its {@code __NEXT_DATA__} carried the tconst) and
     * answered with the WAF challenge's 202; the retriever dropped it and the link was filled from TMDB instead.
     */
    @Test
    public void aRenderedPageArrivingWithTheChallengesStatusIsKept() {
        String page = "<html><script id=\"__NEXT_DATA__\">{\"tconst\":\"tt36586020\"}</script></html>";
        assertEquals(page, OxylabsDocumentRetriever.pageText(result(202, page), URL));
        assertEquals(page, OxylabsDocumentRetriever.pageText(result(200, page), URL));
    }

    @Test
    public void errorsAndNoResultAreNoPage() {
        assertNull(OxylabsDocumentRetriever.pageText(result(403, "blocked"), URL));
        assertNull(OxylabsDocumentRetriever.pageText(result(429, "quota exceeded"), URL));
        assertNull(OxylabsDocumentRetriever.pageText(result(500, "error"), URL));
        assertNull(OxylabsDocumentRetriever.pageText(result(301, ""), URL));
        assertNull(OxylabsDocumentRetriever.pageText(null, URL));
    }
}
