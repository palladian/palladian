package ws.palladian.retrieval;

import org.apache.commons.configuration.BaseConfiguration;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

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

    /** imdb.com picks the page language from this header, so a rendered or a plain fetch must both ask for English. */
    @Test
    public void everyPageIsAskedForInEnglish() {
        BaseConfiguration config = new BaseConfiguration();
        config.addProperty(OxylabsDocumentRetriever.CONFIG_USERNAME, "u");
        config.addProperty(OxylabsDocumentRetriever.CONFIG_PASSWORD, "p");
        OxylabsDocumentRetriever oxylabs = new OxylabsDocumentRetriever(config);
        oxylabs.addNoRenderDomain("inquirer.net");

        for (String page : new String[]{URL, "https://entertainment.inquirer.net/487051/x"}) {
            Map<String, String> headers = oxylabs.buildRequest(page).getHeaders();
            assertEquals("en-US", headers.get("Accept-Language"));
            assertEquals("Oxylabs documents this as what lets a custom header through", "1", headers.get("X-Oxylabs-Force-Headers"));
        }
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
