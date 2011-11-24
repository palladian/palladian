package ws.palladian.retrieval.search;

import java.util.List;

import ws.palladian.retrieval.search.services.WebSearcherLanguage;

/**
 * <p>
 * Interface defining a Web Searcher.
 * </p>
 * 
 * @param <R> The specific type of search result this {@link WebSearcher} implementation provides. This might be page
 *            links, image links, etc.
 * @author Philipp Katz
 */
public interface Searcher<R extends SearchResult> {

    List<R> search(String query);

    List<String> searchUrls(String query);

    int getResultCount(String query);

    void setLanguage(WebSearcherLanguage language);

    WebSearcherLanguage getLanguage();

    void setResultCount(int resultCount);

    int getResultCount();

    String getName();

}
