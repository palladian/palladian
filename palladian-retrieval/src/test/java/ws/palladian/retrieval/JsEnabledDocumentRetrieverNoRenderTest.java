package ws.palladian.retrieval;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Test;
import org.w3c.dom.Document;
import ws.palladian.persistence.json.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The per-domain no-render list ({@link JsEnabledDocumentRetriever#addNoRenderDomain(String)} /
 * {@link JsEnabledDocumentRetriever#shouldRenderJs(String)}) and the cloud retrievers that build their request from
 * it. Offline: only request building is exercised, nothing is sent.
 */
public class JsEnabledDocumentRetrieverNoRenderTest {

    private static JsEnabledDocumentRetriever newRetriever(boolean useJsRendering) {
        JsEnabledDocumentRetriever retriever = new JsEnabledDocumentRetriever() {
            @Override
            public Document getWebDocument(String url) {
                return null;
            }

            @Override
            public int requestsLeft() {
                return Integer.MAX_VALUE;
            }
        };
        retriever.setUseJsRendering(useJsRendering);
        return retriever;
    }

    private static Configuration config(String... keyValues) {
        BaseConfiguration configuration = new BaseConfiguration();
        for (int i = 0; i < keyValues.length; i += 2) {
            configuration.addProperty(keyValues[i], keyValues[i + 1]);
        }
        return configuration;
    }

    @Test
    public void renderingFollowsTheFlagWhenNoDomainIsListed() {
        assertTrue(newRetriever(true).shouldRenderJs("https://entertainment.inquirer.net/487051/x"));
        assertFalse(newRetriever(false).shouldRenderJs("https://entertainment.inquirer.net/487051/x"));
    }

    @Test
    public void listedDomainAndItsSubdomainsAreNotRendered() {
        JsEnabledDocumentRetriever retriever = newRetriever(true);
        retriever.addNoRenderDomain("inquirer.net");

        assertFalse(retriever.shouldRenderJs("https://inquirer.net/"));
        assertFalse(retriever.shouldRenderJs("https://www.inquirer.net/"));
        assertFalse(retriever.shouldRenderJs("https://entertainment.inquirer.net/487051/x"));
        assertFalse(retriever.shouldRenderJs("https://BANDERA.Inquirer.NET/306516/y"));

        assertTrue(retriever.shouldRenderJs("https://www.goodreads.com/book/show/1"));
        assertTrue(retriever.shouldRenderJs("https://inquirer.net.example.com/"));
    }

    @Test
    public void listEntryIsNormalizedLikeTheBlacklist() {
        JsEnabledDocumentRetriever retriever = newRetriever(true);
        retriever.addNoRenderDomain(" www.Goodreads.com ");
        retriever.addNoRenderDomain("https://bandera.inquirer.net/306516/arnel");
        retriever.addNoRenderDomain("amazon.co.uk");

        assertEquals(3, retriever.getNoRenderDomains().size());
        assertTrue(retriever.getNoRenderDomains().contains("goodreads.com"));
        assertTrue(retriever.getNoRenderDomains().contains("inquirer.net"));
        assertTrue(retriever.getNoRenderDomains().contains("amazon.co.uk"));
        assertFalse(retriever.shouldRenderJs("https://www.amazon.co.uk/dp/B000"));
        assertTrue(retriever.shouldRenderJs("https://www.amazon.com/dp/B000"));
    }

    @Test
    public void nullBlankAndUndeterminableInputsAreSafe() {
        JsEnabledDocumentRetriever retriever = newRetriever(true);
        retriever.addNoRenderDomain(null);
        retriever.addNoRenderDomain("  ");
        assertTrue(retriever.getNoRenderDomains().isEmpty());

        // an undeterminable URL keeps the configured behaviour (renders)
        retriever.addNoRenderDomain("inquirer.net");
        assertTrue(retriever.shouldRenderJs(null));
        assertTrue(retriever.shouldRenderJs(""));
        assertTrue(retriever.shouldRenderJs("not a url"));
    }

    @Test
    public void listDoesNotTouchTheBlacklist() {
        JsEnabledDocumentRetriever retriever = newRetriever(true);
        retriever.addNoRenderDomain("inquirer.net");
        assertTrue(retriever.getBlacklistedDomains().isEmpty());
        assertTrue(retriever.shouldExtract("https://entertainment.inquirer.net/487051/x"));
    }

    @Test
    public void oxylabsRendersByDefaultAndDropsTheRenderHeaderForListedDomains() {
        OxylabsDocumentRetriever oxylabs = new OxylabsDocumentRetriever(config(OxylabsDocumentRetriever.CONFIG_USERNAME, "u", OxylabsDocumentRetriever.CONFIG_PASSWORD, "p"));
        assertTrue("Oxylabs must keep rendering unless told otherwise", oxylabs.isUseJsRendering());

        oxylabs.addNoRenderDomain("inquirer.net");

        HttpRequest2 rendered = oxylabs.buildRequest("https://www.goodreads.com/book/show/1");
        assertEquals("html", rendered.getHeaders().get("X-Oxylabs-Render"));
        assertEquals("United States", rendered.getHeaders().get("X-Oxylabs-Geo-Location"));

        HttpRequest2 plain = oxylabs.buildRequest("https://entertainment.inquirer.net/487051/x");
        assertFalse(plain.getHeaders().containsKey("X-Oxylabs-Render"));
        assertEquals("United States", plain.getHeaders().get("X-Oxylabs-Geo-Location"));
        assertEquals("https://entertainment.inquirer.net/487051/x", plain.getUrl());

        oxylabs.setUseJsRendering(false);
        assertFalse(oxylabs.buildRequest("https://www.goodreads.com/book/show/1").getHeaders().containsKey("X-Oxylabs-Render"));
    }

    @Test
    public void brightDataAsksForRenderingOnlyForUnlistedDomains() throws Exception {
        BrightDataDocumentRetriever brightData = new BrightDataDocumentRetriever(config(BrightDataDocumentRetriever.CONFIG_API_KEY, "k", BrightDataDocumentRetriever.CONFIG_ZONE_KEY, "z"));
        brightData.setUseJsRendering(true);
        brightData.addNoRenderDomain("inquirer.net");

        assertEquals(Boolean.TRUE, brightData.buildRequestBody("https://www.goodreads.com/book/show/1").get("js_render"));
        JsonObject plain = brightData.buildRequestBody("https://entertainment.inquirer.net/487051/x");
        assertNull(plain.get("js_render"));
        assertEquals("https://entertainment.inquirer.net/487051/x", plain.getString("url"));
        assertEquals("z", plain.getString("zone"));
    }

    @Test
    public void zyteRequestsRawBodyForListedDomains() {
        ZyteDocumentRetriever zyte = new ZyteDocumentRetriever("k");
        zyte.setUseJsRendering(true);
        zyte.addNoRenderDomain("inquirer.net");

        JsonObject rendered = zyte.buildRequestBody("https://www.goodreads.com/book/show/1");
        assertEquals(Boolean.TRUE, rendered.get("browserHtml"));
        assertNull(rendered.get("httpResponseBody"));

        JsonObject plain = zyte.buildRequestBody("https://entertainment.inquirer.net/487051/x");
        assertEquals(Boolean.TRUE, plain.get("httpResponseBody"));
        assertNull(plain.get("browserHtml"));
    }

    @Test
    public void zyteDecodesTheRawBodyAsUtf8AndPassesBrowserHtmlThrough() throws Exception {
        String html = "<p>Pi\u00f1ol \u201cquoted\u201d \u2014 \u00e9</p>";
        String rawAnswer = "{\"url\":\"u\",\"httpResponseBody\":\"" + Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8)) + "\"}";
        assertEquals(html, ZyteDocumentRetriever.parseResponse(rawAnswer));

        JsonObject renderedAnswer = new JsonObject();
        renderedAnswer.put("browserHtml", html);
        assertEquals(html, ZyteDocumentRetriever.parseResponse(renderedAnswer.toString()));

        assertEquals("not json", ZyteDocumentRetriever.parseResponse("not json"));
    }

    @Test
    public void crawlbaseUsesTheNormalTokenForListedDomains() {
        CrawlbaseDocumentRetriever crawlbase = new CrawlbaseDocumentRetriever(config(CrawlbaseDocumentRetriever.CONFIG_TOKEN_PLAIN, "plain", CrawlbaseDocumentRetriever.CONFIG_TOKEN_JS, "js"));
        crawlbase.setUseJsRendering(true);
        crawlbase.addNoRenderDomain("inquirer.net");

        assertEquals("js", crawlbase.getActiveToken("https://www.goodreads.com/book/show/1"));
        assertEquals("plain", crawlbase.getActiveToken("https://entertainment.inquirer.net/487051/x"));
    }

    @Test
    public void cascadeForwardsListedDomainsToItsCloudRetrievers() {
        JsEnabledDocumentRetriever first = newRetriever(true);
        JsEnabledDocumentRetriever second = newRetriever(true);
        // casts disambiguate the (DocumentRetriever, pool, pool, JsEnabledDocumentRetriever...) constructor; the null
        // cloud entry stands for an unconfigured provider and must be skipped
        CascadingDocumentRetriever cascade = new CascadingDocumentRetriever((DocumentRetriever) null, (RenderingDocumentRetrieverPool) null,
                (RenderingDocumentRetrieverPool) null, first, null, second);
        try {
            cascade.addNoRenderDomain("www.inquirer.net");

            assertEquals(Collections.singleton("inquirer.net"), cascade.getNoRenderDomains());
            for (JsEnabledDocumentRetriever cloud : new JsEnabledDocumentRetriever[]{first, second}) {
                assertFalse(cloud.shouldRenderJs("https://entertainment.inquirer.net/487051/x"));
                assertTrue(cloud.shouldRenderJs("https://www.goodreads.com/book/show/1"));
            }
        } finally {
            cascade.close();
        }
    }
}
