package ws.palladian.retrieval.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ws.palladian.retrieval.resources.WebContent;

public class SearchResults<R extends WebContent> implements Iterable<R> {

    private final List<R> resultList;

    private final Long resultCount;

    public SearchResults(List<R> resultList, Long resultCount) {
        this.resultList = resultList;
        this.resultCount = resultCount;
    }

    public SearchResults(List<R> resultList) {
        this(resultList, null);
    }

    public List<R> getResultList() {
        return resultList;
    }

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

}
