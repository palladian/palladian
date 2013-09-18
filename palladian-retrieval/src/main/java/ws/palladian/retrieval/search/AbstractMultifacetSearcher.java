package ws.palladian.retrieval.search;

import java.util.List;

import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.resources.WebContent;

/**
 * <p>
 * Default implementation for the advanced "multifacet" searchers. Newer searchers should generally derive from here, as
 * the {@link MultifacetQuery} is more flexible; you only need to implement {@link #search(MultifacetQuery)}, and pick
 * the facets you support in your searcher. The old API methods delegate to this one.
 * </p>
 * 
 * @author katz
 * 
 * @param <R>
 */
public abstract class AbstractMultifacetSearcher<R extends WebContent> extends AbstractSearcher<R> {

    @Override
    public final List<R> search(String query, int resultCount, Language language) throws SearcherException {
        MultifacetQuery.Builder builder = new MultifacetQuery.Builder();
        builder.setText(query);
        builder.setResultCount(resultCount);
        builder.setLanguage(language);
        return search(builder.create()).getResultList();
    }

    public abstract SearchResults<R> search(MultifacetQuery query) throws SearcherException;

}
