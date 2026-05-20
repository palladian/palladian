package ws.palladian.retrieval.search.web;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.SearcherException;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DataForSeoSearcherTest {
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void testParseResponse() throws IOException, SearcherException {
        String jsonString = FileHelper.readFileToString(ResourceHelper.getResourceFile("/apiresponse/dataForSeoResponse.json"));
        List<WebContent> results = DataForSeoSearcher.parse(jsonString, 10);

        // 3 organic results in the fixture; the featured_snippet item must be filtered out.
        assertEquals(3, results.size());

        assertEquals("Albert Einstein - Wikipedia", results.get(0).getTitle());
        assertEquals("https://en.wikipedia.org/wiki/Albert_Einstein", results.get(0).getUrl());
        assertEquals("Albert Einstein was a German-born theoretical physicist who developed the theory of relativity, one of the two pillars of modern physics.",
                results.get(0).getSummary());
        assertEquals("DataForSEO", results.get(0).getSource());
        assertEquals(1, results.get(0).getAdditionalData().get("rank_absolute"));
        assertEquals("en.wikipedia.org", results.get(0).getAdditionalData().get("domain"));

        assertEquals("https://www.britannica.com/biography/Albert-Einstein", results.get(1).getUrl());
        assertEquals("https://www.nobelprize.org/prizes/physics/1921/einstein/biographical/", results.get(2).getUrl());
    }

    @Test
    public void testParseResponseRespectsResultCount() throws IOException, SearcherException {
        String jsonString = FileHelper.readFileToString(ResourceHelper.getResourceFile("/apiresponse/dataForSeoResponse.json"));
        List<WebContent> results = DataForSeoSearcher.parse(jsonString, 2);
        assertEquals(2, results.size());
    }

    @Test
    public void testParseErrorResponse() throws IOException {
        String jsonString = FileHelper.readFileToString(ResourceHelper.getResourceFile("/apiresponse/dataForSeoErrorResponse.json"));
        try {
            DataForSeoSearcher.parse(jsonString, 10);
            fail("Expected SearcherException for non-20000 status_code");
        } catch (SearcherException e) {
            assertEquals("Error from DataForSEO API: Authentication failed. Invalid login/password or API key. (status_code=40100).", e.getMessage());
        }
    }

    /**
     * Live integration test — requires real DataForSEO credentials and incurs API cost.
     * Remove {@link Ignore} and provide your credentials to run it.
     */
    @Ignore
    @Test
    public void testSearch() throws Exception {
        String login = "your-login@example.com";
        String password = "your-password";
        DataForSeoSearcher searcher = new DataForSeoSearcher(login, password);
        List<WebContent> results = searcher.search("palladian", 10, Language.ENGLISH);
        collector.checkThat(results.size(), is(10));
        for (WebContent result : results) {
            collector.checkThat(result.getUrl().isEmpty(), is(false));
        }
    }
}
