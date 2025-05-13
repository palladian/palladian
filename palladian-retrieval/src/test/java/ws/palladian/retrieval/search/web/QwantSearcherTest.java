package ws.palladian.retrieval.search.web;

import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.resources.WebContent;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;

/**
 * Created by sky on 19.05.2019.
 */
public class QwantSearcherTest {
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    // ignoring as sometimes we get captcha requests
    @Ignore
    @Test
    public void testSearch() throws Exception {
        QwantSearcher searcher = new QwantSearcher();
        List<WebContent> results = searcher.search("palladian", 15, Language.ENGLISH);
        // CollectionHelper.print(results);
        collector.checkThat(results.size(), Matchers.greaterThan(0));
        for (WebContent result : results) {
            collector.checkThat(result.getUrl().isEmpty(), is(false));
        }
        assertEquals(15, results.size());
        assertEquals("https://en.wikipedia.org/wiki/Palladian_architecture", results.get(0).getUrl());
        assertEquals("Palladian architecture - Wikipedia", results.get(0).getTitle());
    }
}