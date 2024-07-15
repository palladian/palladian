package ws.palladian.retrieval.search.web;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
// import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.resources.WebContent;

import java.util.List;

import static org.hamcrest.core.Is.is;

/**
 * Created by sky on 06.11.2016.
 */
public class DuckDuckGoSearcherTest {
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void testSearch() throws Exception {
        DuckDuckGoSearcher duckDuckGoSearcher = new DuckDuckGoSearcher();
        List<WebContent> results = duckDuckGoSearcher.search("palladian", 10, Language.ENGLISH);
        // CollectionHelper.print(results);
        collector.checkThat(results.size(), is(10));
        for (WebContent result : results) {
            collector.checkThat(result.getUrl().isEmpty(), is(false));
        }
    }
}