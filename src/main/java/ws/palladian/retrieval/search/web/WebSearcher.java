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

    protected final DocumentRetriever retriever;

    private int resultCount;

    private WebSearcherLanguage language;

    public WebSearcher() {
        retriever = new DocumentRetriever();
        resultCount = 10;
        language = WebSearcherLanguage.ENGLISH;
    }

    /**
     * <p>
     * Convenience method to retrieve a list of URLs for the specified query instead of {@link SearchResult}s.
     * </p>
     * 
     * @param query
     * @return
     */
    public List<String> searchUrls(String query) {
        List<String> urls = new ArrayList<String>();

        List<R> webresults = search(query);
        for (R webresult : webresults) {
            String url = webresult.getUrl();
            if (url != null) {
                urls.add(url);
            }
        }

        return urls;
    }

    @Override
    public int getTotalResultCount(String query) {
        throw new UnsupportedOperationException("not supported for this searcher");
    }

    /**
     * @return the resultCount
     */
    @Override
    public final int getResultCount() {
        return resultCount;
    }

    /**
     * @param resultCount the resultCount to set
     */
    @Override
    public final void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    /**
     * <p>
     * Get the language for the search.
     * </p>
     * 
     * @return
     */
    public final WebSearcherLanguage getLanguage() {
        return language;
    }

    /**
     * <p>
     * Specify the language for the search.
     * </p>
     * 
     * @param language
     */
    public final void setLanguage(WebSearcherLanguage language) {
        this.language = language;
    }

    /**
     * <p>
     * Get the total number of requests, which this class of web searcher has performed. As usage limitations apply to
     * the number of HTTP requests, this is the number of actual HTTP requests, <b>not</b> the number of queries. For
     * example, a query with 1.000 desired results requires 10 HTTP requests to the service, each returning 100 results.
     * In this case, the number of requests should be incremented by 10.
     * </p>
     * 
     * <p>
     * When creating your own implementations, keep in mind, that usage restrictions usually apply site-wide, so the
     * counter should be implemented in the base-class of each service, not individually for all its subclasses.
     * </p>
     * 
     * @return
     */
    public abstract int getRequestCount();
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append(":").append(getRequestCount());
        return sb.toString();
    }

}
