package ws.palladian.retrieval.search;

import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.configuration.ConfigurationOption;
import ws.palladian.retrieval.resources.WebContent;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * Interface defining a {@link Searcher}. A Searcher might be an implementation for a web search engine like Google,
 * Bing, etc. On the other hand, a Searcher might perform queries on local, file-system-based indices like Lucene.
 * Searchers can be queried with information requests and return subclasses of {@link WebContent}s. <b>Hint:</b> Use
 * {@link AbstractSearcher} or {@link AbstractMultifacetSearcher} as base class for implementations of this interface.
 * </p>
 *
 * @param <R> The specific type of search result this {@link Searcher} implementation provides. This might be page
 *            links, image links, full documents, etc.
 * @author Philipp Katz
 */
public interface Searcher<R extends WebContent> {

    /**
     * Meta information and factory for a searcher service. It describes the searcher
     * result types, configuration options, and allows to instantiate the service.
     *
     * @since 3.0.0
     */
    public interface SearcherMetaInfo<S extends Searcher<C>, C extends WebContent> {
        /** @return The human-readable name of the ranking service. */
        String getSearcherName();

        /** @return The ID of this ranking service. */
        String getSearcherId();

        /** @return The type of result which this searcher produces. */
        Class<C> getResultType();

        /** @return Config options which are required for this ranking service. */
        List<ConfigurationOption<?>> getConfigurationOptions();

        /**
         * Instantiate a new searcher.
         *
         * @param config The configuration (see {@link #getConfigurationOptions()})
         * @return The ranking service instance.
         */
        S create(Map<ConfigurationOption<?>, ?> config);

        /**
         * @return <code>true</code> in case the use of this {@link Searcher} is
         *         deprecated, e.g. the API is not supported any more or it is based on
         *         an unofficial API.
         */
        default boolean isDeprecated() {
            return false;
        }

        /**
         * @return A URL which represents the given searcher, respectively API - ideally
         *         it should link to a page with the API specification and / or a place
         *         where the user can create an API key. If no such documentation
         *         exists, return `null`.
         */
        String getSearcherDocumentationUrl();

        /**
         * @return A short and to the point, description of this service (allow inline
         *         HTML elements). At least one full grammatically correct English
         *         sentence ending with a full stop. If no description is available,
         *         return `null`.
         */
        String getSearcherDescription();
    }

    /**
     * The default language to use for search.
     */
    Language DEFAULT_SEARCHER_LANGUAGE = Language.ENGLISH;

    /**
     * <p>
     * Retrieve a list of {@link WebContent}s for the specified query.
     * </p>
     *
     * @param query       The text for which to search, not <code>null</code> or empty.
     * @param resultCount Maximum number of results to retrieve.
     * @return A list of results, never <code>null</code>.
     * @throws SearcherException In case the search fails.
     */
    List<R> search(String query, int resultCount) throws SearcherException;

    /**
     * <p>
     * Retrieve a list of {@link WebContent}s for the specified query.
     * </p>
     *
     * @param query       The text for which to search, not <code>null</code> or empty.
     * @param resultCount Maximum number of results to retrieve.
     * @param language    The language for which to search, not <code>null</code>.
     * @return A list of results, never <code>null</code>.
     * @throws SearcherException In case the search fails.
     */
    List<R> search(String query, int resultCount, Language language) throws SearcherException;

    /**
     * <p>
     * Convenience method to retrieve a list of URLs for the specified query instead of {@link WebContent}s.
     * </p>
     *
     * @param query       The text for which to search, not <code>null</code> or empty.
     * @param resultCount Maximum number of results to retrieve.
     * @return A list of URLs, never <code>null</code>.
     * @throws SearcherException In case the search fails.
     */
    List<String> searchUrls(String query, int resultCount) throws SearcherException;

    /**
     * <p>
     * Convenience method to retrieve a list of URLs for the specified query instead of {@link WebContent}s.
     * </p>
     *
     * @param query       The text for which to search, not <code>null</code> or empty.
     * @param resultCount Maximum number of results to retrieve.
     * @param language    The language for which to search, not <code>null</code>.
     * @return A list of URLs, never <code>null</code>.
     * @throws SearcherException In case the search fails.
     */
    List<String> searchUrls(String query, int resultCount, Language language) throws SearcherException;

    /**
     * <p>
     * Get the total number of results available for the specified query.
     * </p>
     *
     * @param query The text for which to search, not <code>null</code> or empty.
     * @return The number of available search results for the given query.
     * @throws SearcherException In case the search fails, or the searcher does not allow to retrieve this information.
     */
    long getTotalResultCount(String query) throws SearcherException;

    /**
     * <p>
     * Override, if this searcher supports getting the total number of available results.
     * </p>
     *
     * @param query    The text for which to search, not <code>null</code> or empty.
     * @param language The language for which to search, not <code>null</code>.
     * @return The number of available search results for the given query.
     * @throws SearcherException In case the search fails, or the searcher does not allow to retrieve this information.
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
     * @return A human-readable description for this {@link Searcher}.
     * @deprecated Get this from {@link SearcherMetaInfo}
     */
    @Deprecated
    String getName();

    /**
     * @return <code>true</code> in case the use of this {@link Searcher} is deprecated, e.g. the API is not supported
     * any more or it is based on an unofficial API.
     * @deprecated Get this from {@link SearcherMetaInfo}
     */
    @Deprecated
    boolean isDeprecated();
}
