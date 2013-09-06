package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.search.SearchResult;
import ws.palladian.retrieval.search.Searcher;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Base implementation for a {@link WebSearcher} providing common functionality. A WebSearcher is a component which
 * queries APIs from web search engines, like Google.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class WebSearcher<R extends WebResult> implements Searcher<R> {

    private static final Language DEFAULT_SEARCHER_LANGUAGE = Language.ENGLISH;

    protected final HttpRetriever retriever;

    public WebSearcher() {
        retriever = HttpRetrieverFactory.getHttpRetriever();
    }

    /**
     * <p>
     * Convenience method to retrieve a list of URLs for the specified query instead of {@link SearchResult}s.
     * </p>
     * 
     * @param query
     * @param resultCount Maximum number of results to retrieve.
     * @return
     * @throws SearcherException In case the search fails.
     */
    public final List<String> searchUrls(String query, int resultCount) throws SearcherException {
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
     * @throws SearcherException In case the search fails.
     */
    public final List<String> searchUrls(String query, int resultCount, Language language) throws SearcherException {
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
    public final List<R> search(String query, int resultCount) throws SearcherException {
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
     * @throws SearcherException In case the search fails.
     */
    public abstract List<R> search(String query, int resultCount, Language language) throws SearcherException;

    @Override
    public final int getTotalResultCount(String query) throws SearcherException {
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
     * @throws SearcherException In case the search fails.
     */
    public int getTotalResultCount(String query, Language language) throws SearcherException {
        throw new SearcherException("Obtaining the total number of results is not supported or implemented by "
                + getName() + ".");
    }

    @Override
    public String toString() {
        return getName();
    }

}
