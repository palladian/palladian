package ws.palladian.retrieval.search;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.resources.WebContent;

/**
 * <p>
 * Base implementation for a {@link Searcher} providing common functionality. A Searcher is a component which queries
 * APIs from web search engines, like Google. Implementation hints: At least {@link #search(String, int, Language)} must
 * be implemented. In case the searcher supports retrieving the number of available search results for a query, also
 * override {@link #getTotalResultCount(String, Language)} (if not overridden, this will trigger a
 * {@link SearcherException} else wise). <b>Important:</b> From now on, new {@link Searcher} implementations should use
 * {@link AbstractMultifacetSearcher} as a foundation.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class AbstractSearcher<R extends WebContent> implements Searcher<R> {

    @Override
    public final List<String> searchUrls(String query, int resultCount) throws SearcherException {
        return searchUrls(query, resultCount, DEFAULT_SEARCHER_LANGUAGE);
    }

    @Override
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

    @Override
    public final long getTotalResultCount(String query) throws SearcherException {
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
    @Override
    public long getTotalResultCount(String query, Language language) throws SearcherException {
        throw new SearcherException("Obtaining the total number of results is not supported or implemented by "
                + getName() + ".");
    }

    /**
     * Default implementation which just delegates to the old API (only text query and language).
     */
    @Override
    public SearchResults<R> search(MultifacetQuery query) throws SearcherException {
        String queryText = query.getText();
        if (queryText == null || queryText.isEmpty()) {
            throw new SearcherException("For this searcher, the query must provide text.");
        }
        return new SearchResults<R>(search(queryText, query.getResultCount(), query.getLanguage()));
    }

    @Override
    public String toString() {
        return getName();
    }

}
