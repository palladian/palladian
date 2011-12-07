package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.search.SearchResult;
import ws.palladian.retrieval.search.Searcher;

/**
 * <p>
 * Base implementation for a {@link WebSearcher} providing common functionality. A WebSearcher is a component which
 * queries APIs from web search engines, like Google.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class WebSearcher<R extends WebResult> implements Searcher<R> {

    private static final WebSearcherLanguage DEFAULT_SEARCHER_LANGUAGE = WebSearcherLanguage.ENGLISH;

    protected final DocumentRetriever retriever;

    public WebSearcher() {
        retriever = new DocumentRetriever();
    }

    /**
     * <p>
     * Convenience method to retrieve a list of URLs for the specified query instead of {@link SearchResult}s.
     * </p>
     * 
     * @param query
     * @param resultCount Maximum number of results to retrieve.
     * @return
     */
    public List<String> searchUrls(String query, int resultCount) {
        return searchUrls(query, resultCount, DEFAULT_SEARCHER_LANGUAGE);
    }

    /**
     * <p>
     * Convenience method to retrieve a list of URLs for the specified query instead of {@link SearchResult}s.
     * </p>
     * 
     * @param query
     * @param resultCount Maximum number of results to retrieve.
     * @param language The language for which to search.
     * @return
     */
    public List<String> searchUrls(String query, int resultCount, WebSearcherLanguage language) {
        List<String> urls = new ArrayList<String>();

        List<R> webresults = search(query, resultCount, language);
        for (R webresult : webresults) {
            String url = webresult.getUrl();
            if (url != null) {
                urls.add(url);
            }
        }

        return urls;
    }

    @Override
    public List<R> search(String query, int resultCount) {
        return search(query, resultCount, DEFAULT_SEARCHER_LANGUAGE);
    }

    /**
     * <p>
     * Retrieve a list of {@link SearchResult}s for the specified query.
     * </p>
     * 
     * @param query
     * @param resultCount Maximum number of results to retrieve.
     * @param language The language for which to search.
     * @return
     */
    public abstract List<R> search(String query, int resultCount, WebSearcherLanguage language);

    @Override
    public int getTotalResultCount(String query) {
        return getTotalResultCount(query, DEFAULT_SEARCHER_LANGUAGE);
    }

    /**
     * <p>
     * Override, if this searcher supports getting the total number of available results.
     * </p>
     * 
     * @param query
     * @param language
     * @return
     */
    public int getTotalResultCount(String query, WebSearcherLanguage language) {
        throw new UnsupportedOperationException("not supported for this searcher");
    }

    @Override
    public String toString() {
        return getName();
    }

}
