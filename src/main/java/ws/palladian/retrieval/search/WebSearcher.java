package ws.palladian.retrieval.search;

import java.util.List;

import ws.palladian.retrieval.search.services.WebSearcherLanguage;

/**
 * <p>Interface defining a Searcher.</p>
 * 
 * @author Philipp Katz
 */
public interface WebSearcher {

    List<WebResult> search(String query);

    List<String> searchUrls(String query);

    int getResultCount(String query);

    void setLanguage(WebSearcherLanguage language);

    WebSearcherLanguage getLanguage();

    void setResultCount(int resultCount);

    int getResultCount();

    String getName();

}
