package ws.palladian.retrieval.search.services;

import java.util.List;

import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.search.WebResult;

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
