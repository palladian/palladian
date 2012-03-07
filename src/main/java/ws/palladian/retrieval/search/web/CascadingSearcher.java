package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.collection.CollectionHelper;
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
    public List<WebResult> search(String query, int resultCount, WebSearcherLanguage language) throws SearcherException {

        List<WebResult> webResults = new ArrayList<WebResult>();

        for (WebSearcher<WebResult> searcher : searchers) {
            webResults.addAll(searcher.search(query, resultCount, language));

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
        searchers.add(new BlekkoSearcher(ConfigHolder.getInstance().getConfig()));
        searchers.add(new ScroogleSearcher());
        searchers.add(new GoogleSearcher());

        CascadingSearcher cs = new CascadingSearcher(searchers);
        List<WebResult> results = cs.search("the population of germany", 11);
        CollectionHelper.print(results);
    }


}
