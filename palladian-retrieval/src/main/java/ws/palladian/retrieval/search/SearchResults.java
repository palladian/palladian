package ws.palladian.retrieval.search;

import org.apache.commons.lang3.Validate;
import ws.palladian.retrieval.resources.WebContent;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * Search results from a {@link Searcher}. It optionally provides the number of total available results for the query
 * using {@link #getResultCount()}.
 * </p>
 *
 * @param <R>
 * @author Philipp Katz
 */
public class SearchResults<R extends WebContent> implements Iterable<R> {
    private final List<R> resultList;

    private final Long totalResultCount;

    public SearchResults(List<R> resultList, Long totalResultCount) {
        Validate.notNull(resultList, "resultList must not be null");
        this.resultList = resultList;
        this.totalResultCount = totalResultCount;
    }

    public SearchResults(List<R> resultList) {
        this(resultList, null);
    }

    /**
     * @return The list with results.
     */
    public List<R> getResultList() {
        return resultList;
    }

    /**
     * @return A list of URLs from the results.
     */
    public List<String> getResultUrls() {
        return resultList.stream().map(R::getUrl).collect(Collectors.toList());
    }

    /**
     * @return The number of total available results, or <code>null</code> in case this information is not provided by
     * the searcher.
     * @deprecated Use {@link #getTotalResultCount()} instead.
     */
    @Deprecated
    public Long getResultCount() {
        return totalResultCount;
    }

    /**
     * @return The number of total available results, or <code>null</code> in case this information is not provided by
     * the searcher.
     */
    public Long getTotalResultCount() {
        return totalResultCount;
    }

    @Override
    public Iterator<R> iterator() {
        return resultList.iterator();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SearchResults [#results=");
        builder.append(resultList.size());
        if (totalResultCount != null) {
            builder.append(", #totalResults=");
            builder.append(totalResultCount);
        }
        builder.append("]");
        return builder.toString();
    }
}
