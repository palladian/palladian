package ws.palladian.retrieval.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.retrieval.resources.WebContent;

/**
 * <p>
 * Search results from a {@link Searcher}. It optionally provides the number of total available results for the query
 * using {@link #getResultCount()}.
 * </p>
 * 
 * @author pk
 * @param <R>
 */
public class SearchResults<R extends WebContent> implements Iterable<R> {

    private final List<R> resultList;

    private final Long resultCount;

    public SearchResults(List<R> resultList, Long resultCount) {
        Validate.notNull(resultList, "resultList must not be null");
        this.resultList = resultList;
        this.resultCount = resultCount;
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
        List<String> urls = new ArrayList<String>();

        for (R searchResult : this) {
            if (searchResult.getUrl() != null) {
                urls.add(searchResult.getUrl());
            }
        }

        return urls;
    }

    /**
     * @return The number of total available results, or <code>null</code> in case this information is not provided by
     *         the searcher.
     */
    public Long getResultCount() {
        return resultCount;
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
        builder.append(", #totalResults=");
        builder.append(resultCount);
        builder.append("]");
        return builder.toString();
    }

}
