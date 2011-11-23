package ws.palladian.retrieval.search.services;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.search.WebResult;
import ws.palladian.retrieval.search.WebSearcher;

/**
 * Base implementation for a {@link WebSearcher} providing common functionality.
 * 
 * @author Philipp Katz
 */
public abstract class BaseWebSearcher implements WebSearcher {

    protected final DocumentRetriever retriever;

    private int resultCount;

    private WebSearcherLanguage language;

    public BaseWebSearcher() {
        retriever = new DocumentRetriever();
        resultCount = 10;
        language = WebSearcherLanguage.ENGLISH;
    }

    @Override
    public List<WebResult> search(String query) {
        return search(query, getResultCount());
    }

    @Override
    public List<WebResult> search(String query, int resultCount) {
        return search(query, resultCount, getLanguage());
    }

    @Override
    public List<WebResult> search(String query, WebSearcherLanguage language) {
        return search(query, getResultCount(), language);
    }

    @Override
    public List<String> searchUrls(String query) {
        return searchUrls(query, getResultCount());
    }

    @Override
    public List<String> searchUrls(String query, int resultCount) {
        return searchUrls(query, resultCount, getLanguage());
    }

    @Override
    public List<String> searchUrls(String query, WebSearcherLanguage language) {
        return searchUrls(query, getResultCount(), language);
    }

    @Override
    public List<String> searchUrls(String query, int resultCount, WebSearcherLanguage language) {
        List<String> urls = new ArrayList<String>();

        List<WebResult> webresults = search(query, resultCount, language);
        for (WebResult webresult : webresults) {
            urls.add(webresult.getUrl());
        }

        return urls;
    }

    @Override
    public int getResultCount(String query) {
        throw new UnsupportedOperationException("not supported for this searcher");
    }

    /**
     * @return the resultCount
     */
    @Override
    public final int getResultCount() {
        return resultCount;
    }

    /**
     * @param resultCount the resultCount to set
     */
    @Override
    public final void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    /**
     * @return the language
     */
    @Override
    public final WebSearcherLanguage getLanguage() {
        return language;
    }

    /**
     * @param language the language to set
     */
    @Override
    public final void setLanguage(WebSearcherLanguage language) {
        this.language = language;
    }

}
