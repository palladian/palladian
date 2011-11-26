package ws.palladian.retrieval.search;

import java.util.List;

import ws.palladian.retrieval.search.services.WebSearcherLanguage;

/**
 * <p>
 * Interface defining a {@link Searcher}. A Searcher might be an implementation for a web search engine like Google,
 * Bing, etc. On the other hand, a Searcher might perform queries on local, file-system-based indices like Lucene.
 * Searchers can be queried with information requests and return subclasses of {@link SearchResult}s.
 * </p>
 * 
 * @param <R> The specific type of search result this {@link Searcher} implementation provides. This might be page
 *            links, image links, full documents, etc.
 * @author Philipp Katz
 */
public interface Searcher<R extends SearchResult> {

    /**
     * <p>
     * Retrieve a list of {@link SearchResult}s for the specified query.
     * </p>
     * 
     * @param query
     * @return
     */
    List<R> search(String query);

    /**
     * <p>
     * Convenience method to retrieve a list of URLs for the specified query instead of {@link SearchResult}s.
     * </p>
     * 
     * @param query
     * @return
     */
    // TODO push down
    List<String> searchUrls(String query);

    /**
     * <p>
     * Get the total number of results available for the specified query.
     * </p>
     * 
     * @param query
     * @return
     */
    int getTotalResultCount(String query);

    /**
     * <p>
     * Specify the language for the search.
     * </p>
     * 
     * @param language
     */
    // TODO push down
    void setLanguage(WebSearcherLanguage language);

    /**
     * <p>
     * Get the language for the search.
     * </p>
     * 
     * @return
     */
    // TODO push down
    WebSearcherLanguage getLanguage();

    /**
     * <p>
     * Specify the number of desired results for the queries.
     * </p>
     * 
     * @param resultCount
     */
    void setResultCount(int resultCount);

    /**
     * <p>
     * Get the number of desired results for the queries.
     * </p>
     * 
     * @return
     */
    int getResultCount();

    /**
     * <p>
     * Get a human-readable description for this {@link Searcher}.
     * </p>
     * 
     * @return
     */
    String getName();

}
