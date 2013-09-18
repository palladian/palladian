package ws.palladian.retrieval.search;

import java.util.List;

import javax.naming.directory.SearchResult;

import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.resources.WebContent;

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
public interface Searcher<R extends WebContent> {
	
	/** The default language to use for search. */
    public static final Language DEFAULT_SEARCHER_LANGUAGE = Language.ENGLISH;

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
     * Retrieve a list of {@link SearchResult}s for the specified query.
     * </p>
     * 
     * @param query
     * @param resultCount Maximum number of results to retrieve.
     * @param language The language for which to search.
     * @return
     * @throws SearcherException In case the search fails.
     */
    List<R> search(String query, int resultCount, Language language) throws SearcherException;
    
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
    List<String> searchUrls(String query, int resultCount) throws SearcherException;
    
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
    List<String> searchUrls(String query, int resultCount, Language language) throws SearcherException;

    /**
     * <p>
     * Get the total number of results available for the specified query.
     * </p>
     * 
     * @param query
     * @return
     * @throws SearcherException In case the search fails.
     */
    long getTotalResultCount(String query) throws SearcherException;
    
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
    long getTotalResultCount(String query, Language language) throws SearcherException;
    
	/**
	 * <p>
	 * Do a search with a {@link MultifacetQuery}. In case, a facet is not supported, it is simply ignored.
	 * </p>
	 * 
	 * @param query The query, not <code>null</code>.
	 * @return The results for the query, never <code>null</code>.
	 * @throws SearcherException In case the search fails.
	 */
	SearchResults<R> search(MultifacetQuery query) throws SearcherException;

    /**
     * <p>
     * Get a human-readable description for this {@link Searcher}.
     * </p>
     * 
     * @return
     */
    String getName();

}
