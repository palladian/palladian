package ws.palladian.retrieval.search.services;

import java.util.List;

import ws.palladian.retrieval.search.WebResult;

public interface WebSearcher {

    List<WebResult> search(String query, int resultCount, WebSearcherLanguage language);

    List<WebResult> search(String query, int resultCount);

    List<WebResult> search(String query, WebSearcherLanguage language);

    List<WebResult> search(String query);

    List<String> searchUrls(String query, int resultCount, WebSearcherLanguage language);

    List<String> searchUrls(String query, int resultCount);

    List<String> searchUrls(String query, WebSearcherLanguage language);

    List<String> searchUrls(String query);
    
    int getHitCount(String query);

    void setLanguage(WebSearcherLanguage language);

    WebSearcherLanguage getLanguage();

    void setResultCount(int resultCount);

    int getResultCount();

    String getName();
    
    // TODO add method for exact searches?

}
