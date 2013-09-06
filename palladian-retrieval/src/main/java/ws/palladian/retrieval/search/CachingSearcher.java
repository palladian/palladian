package ws.palladian.retrieval.search;

import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.MruMap;

/**
 * <p>
 * Cache for an arbitrary {@link Searcher}.
 * </p>
 * 
 * @author Philipp Katz
 * @param <R> The result type of the {@link Searcher}.
 */
public class CachingSearcher<R extends SearchResult> implements Searcher<R> {

    private final Searcher<R> searcher;

    private final MruMap<String, List<R>> searchCache;

    private final MruMap<String, Long> countCache;

    /**
     * <p>
     * Create a new {@link CachingSearcher}, wrapping another {@link Searcher} (decorator pattern).
     * </p>
     * 
     * @param cacheSize Size of the cache, greater zero.
     * @param searcher The searcher to wrap, not <code>null</code>.
     */
    public CachingSearcher(int cacheSize, Searcher<R> searcher) {
        Validate.isTrue(cacheSize > 0, "cacheSize must be greater zero");
        Validate.notNull(searcher, "searcher must not be null");
        this.searcher = searcher;
        searchCache = new MruMap<String, List<R>>(cacheSize);
        countCache = new MruMap<String, Long>(cacheSize);
    }

    @Override
    public List<R> search(String query, int resultCount) throws SearcherException {
        String identifier = query + "####" + resultCount;
        List<R> result = searchCache.get(identifier);
        if (result == null) {
            result = searcher.search(query, resultCount);
            searchCache.put(identifier, result);
        }
        return result;
    }

    @Override
    public long getTotalResultCount(String query) throws SearcherException {
        Long result = countCache.get(query);
        if (result == null) {
            result = searcher.getTotalResultCount(query);
            countCache.put(query, result);
        }
        return result;
    }

    @Override
    public String getName() {
        return searcher.getName() + " (cached)";
    }

}
