package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.search.SearchResult;
import ws.palladian.retrieval.search.Searcher;

/**
 * Base implementation for a {@link WebSearcher} providing common functionality.
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

}
