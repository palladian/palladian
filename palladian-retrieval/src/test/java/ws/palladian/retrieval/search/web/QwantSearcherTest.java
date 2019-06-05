package ws.palladian.retrieval.search.web;

import static org.hamcrest.core.Is.is;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.resources.WebContent;

/**
 * Created by sky on 19.05.2019.
 */
public class QwantSearcherTest {
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void testSearch() throws Exception {
        QwantSearcher searcher = new QwantSearcher();
        List<WebContent> results = searcher.search("palladian", 10, Language.ENGLISH);
        CollectionHelper.print(results);
        for (WebContent result : results) {
            collector.checkThat(result.getUrl().isEmpty(), is(false));
        }
    }
}