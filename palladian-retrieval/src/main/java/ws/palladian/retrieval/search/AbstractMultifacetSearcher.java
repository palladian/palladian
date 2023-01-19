package ws.palladian.retrieval.search;

import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.resources.WebContent;

import java.util.List;

/**
 * <p>
 * Default implementation for the advanced "multifacet" searchers. Newer searchers should generally derive from here, as
 * the {@link MultifacetQuery} is more flexible; you only need to implement {@link #search(MultifacetQuery)}, and pick
 * the facets you support in your searcher. The old API methods delegate to this one.
 * </p>
 *
 * @param <R>
 * @author Philipp Katz
 */
public abstract class AbstractMultifacetSearcher<R extends WebContent> extends AbstractSearcher<R> {
    @Override
    public final List<R> search(String query, int resultCount, Language language) throws SearcherException {
        MultifacetQuery.Builder builder = new MultifacetQuery.Builder();
        builder.setText(query);
        if (defaultResultCount != null) {
            builder.setResultCount(defaultResultCount);
        } else {
            builder.setResultCount(resultCount);
        }
        builder.setLanguage(language);
        return search(builder.create()).getResultList();
    }

    @Override
    public final long getTotalResultCount(String query, Language language) throws SearcherException {
        MultifacetQuery.Builder builder = new MultifacetQuery.Builder();
        builder.setText(query);
        builder.setLanguage(language);
        builder.setResultCount(1);
        Long resultCount = search(builder.create()).getResultCount();
        if (resultCount == null) {
            throw new SearcherException("Obtaining the total number of results is not supported or implemented by " + getName() + ".");
        }
        return resultCount;
    }

    /** To be overridden by implementations. */
    public abstract SearchResults<R> search(MultifacetQuery query) throws SearcherException;

}
