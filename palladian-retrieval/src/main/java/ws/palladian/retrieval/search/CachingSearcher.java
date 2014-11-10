package ws.palladian.retrieval.search;

import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.LruMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.resources.WebContent;

/**
 * <p>
 * Cache for an arbitrary {@link Searcher}.
 * </p>
 * 
 * @author Philipp Katz
 * @param <R> The result type of the {@link Searcher}.
 */
public class CachingSearcher<R extends WebContent> extends AbstractSearcher<R> {

    private final Searcher<R> searcher;

    private final LruMap<String, List<R>> searchCache;

    private final LruMap<String, Long> countCache;

    /**
     * <p>
     * Create a new {@link CachingSearcher}, wrapping another {@link Searcher} (decorator pattern).
     * </p>
     * 
     * @param cacheSize Size of the cache, greater zero.
     * @param searcher The searcher to wrap, not <code>null</code>.
     * @return A caching searcher for the provided searcher.
     */
    public static <R extends WebContent> CachingSearcher<R> create(int cacheSize, Searcher<R> searcher) {
        Validate.isTrue(cacheSize > 0, "cacheSize must be greater zero");
        Validate.notNull(searcher, "searcher must not be null");
        return new CachingSearcher<R>(cacheSize, searcher);
    }

    private CachingSearcher(int cacheSize, Searcher<R> searcher) {
        this.searcher = searcher;
        searchCache = LruMap.insertionOrder(cacheSize);
        countCache = LruMap.insertionOrder(cacheSize);
    }

    @Override
    public List<R> search(String query, int resultCount, Language language) throws SearcherException {
        String identifier = language.getIso6391() + "####" + query + "####" + resultCount;
        List<R> result = searchCache.get(identifier);
        if (result == null) {
            result = searcher.search(query, resultCount, language);
            searchCache.put(identifier, result);
        }
        return result;
    }

    @Override
    public long getTotalResultCount(String query, Language language) throws SearcherException {
        String identifier = language.getIso6391() + "####" + query;
        Long result = countCache.get(identifier);
        if (result == null) {
            result = searcher.getTotalResultCount(query, language);
            countCache.put(identifier, result);
        }
        return result;
    }

    @Override
    public String getName() {
        return searcher.getName() + " (cached)";
    }

}
