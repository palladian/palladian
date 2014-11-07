package ws.palladian.retrieval.search.news;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.SearcherException;

@SuppressWarnings("deprecation")
public class GoogleNewsSearcherTest {

    @Test
    @Ignore
    public void testGoogleNews_date_issue288() throws SearcherException {
        GoogleNewsSearcher searcher = new GoogleNewsSearcher();
        List<WebContent> results = searcher.search("putin", 10);
        CollectionHelper.print(results);
    }

    @Test
    public void testParseDate() {
        String dateString = "Mon, 21 Apr 2014 16:15:00 -0700";
        assertNotNull(GoogleNewsSearcher.parseDate(dateString));
    }

}
