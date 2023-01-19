package ws.palladian.retrieval.search.web;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.resources.WebContent;

import java.util.List;

import static org.hamcrest.Matchers.is;

/**
 * <p>Created by David Urbansky on 29.09.2015.</p>
 *
 * @author David Urbansky
 */
@Ignore
public class WebKnoxSearcherTest {

    private static final String API_KEY = "XXX";

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void testSearch() throws Exception {

        WebKnoxSearcher webKnoxSearcher = new WebKnoxSearcher(API_KEY);
        List<WebContent> results = webKnoxSearcher.search("web such api", 5, Language.GERMAN);
        CollectionHelper.print(results);
        collector.checkThat(results.size(), is(5));

        webKnoxSearcher = new WebKnoxSearcher(API_KEY);
        results = webKnoxSearcher.search("search api", 5, Language.ENGLISH);
        CollectionHelper.print(results);
        collector.checkThat(results.size(), is(5));

    }
}