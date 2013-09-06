package ws.palladian.retrieval.search;

import java.util.List;

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
     * @param resultCount
     * @return
     * @throws SearcherException In case the search fails.
     */
    List<R> search(String query, int resultCount) throws SearcherException; 

    /**
     * <p>
     * Get the total number of results available for the specified query.
     * </p>
     * 
     * @param query
     * @return
     * @throws SearcherException In case the search fails.
     */
    int getTotalResultCount(String query) throws SearcherException;

    /**
     * <p>
     * Get a human-readable description for this {@link Searcher}.
     * </p>
     * 
     * @return
     */
    String getName();

}
