package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * The cascading searcher can be used to query multiple search engines in a specified order. If one engine does not
 * return any results the next one will be queried. This is helpful to bypass rate limits and have a fallback.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class CascadingSearcher extends WebSearcher<WebResult> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(CascadingSearcher.class);

    private List<WebSearcher<WebResult>> searchers;

    public CascadingSearcher(List<WebSearcher<WebResult>> searchers) {
        this.searchers = searchers;
        if (searchers == null) {
            searchers = new ArrayList<WebSearcher<WebResult>>();
        }
    }

    public void addSearcher(WebSearcher<WebResult> searcher) {
        this.searchers.add(searcher);
    }

    @Override
    public String getName() {
        return "Cascading Searcher";
    }

    @Override
    public List<WebResult> search(String query, int resultCount, Language language) {

        List<WebResult> webResults = new ArrayList<WebResult>();

        for (WebSearcher<WebResult> searcher : searchers) {

            // some searchers go haywire if something goes wrong, so we catch the exception and try the next one
            try {
                webResults.addAll(searcher.search(query, resultCount, language));
            } catch (SearcherException e) {
                LOGGER.error(e.getMessage());
            }

            // stop as soon as one searcher found something
            if (!webResults.isEmpty()) {
                break;
            }

        }

        return webResults;
    }

    /**
     * <p>
     * Usage Example
     * </p>
     * 
     * @param args
     * @throws SearcherException
     */
    public static void main(String[] args) throws SearcherException {
        List<WebSearcher<WebResult>> searchers = new ArrayList<WebSearcher<WebResult>>();
        // searchers.add(new YandexSearcher(
        // "http://xmlsearch.yandex.ru/xmlsearch?user=pkatz&key=03.156690494:67abdff20756319b24dc308f8d216e22")); // 2.2
        // searchers.add(new HakiaSearcher(ConfigHolder.getInstance().getConfig())); // 6s
        // searchers.add(new DuckDuckGoSearcher()); // 3.1s
        // searchers.add(new BingSearcher(ConfigHolder.getInstance().getConfig())); // 4.4s
        // searchers.add(new ScroogleSearcher()); // 6.5s
        // searchers.add(new BlekkoSearcher(ConfigHolder.getInstance().getConfig())); // 11.2s
        searchers.add(new GoogleSearcher()); // 2.7s

        StopWatch stopWatch = new StopWatch();
        CascadingSearcher cs = new CascadingSearcher(searchers);
        for (int i = 0; i < 1; i++) {
            List<WebResult> results = cs.search("\"Sony Ericsson T230i\" \"talk time\"", 10);
            CollectionHelper.print(results);
        }
        System.out.println(stopWatch.getElapsedTimeString());
    }
}