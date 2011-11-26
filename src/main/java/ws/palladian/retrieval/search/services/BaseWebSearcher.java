package ws.palladian.retrieval.search.services;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.search.Searcher;
import ws.palladian.retrieval.search.WebResult;

/**
 * Base implementation for a {@link WebSearcher} providing common functionality.
 * 
 * @author Philipp Katz
 */
public abstract class BaseWebSearcher<R extends WebResult> implements Searcher<R> {

    protected final DocumentRetriever retriever;

    private int resultCount;

    private WebSearcherLanguage language;

    public BaseWebSearcher() {
        retriever = new DocumentRetriever();
        resultCount = 10;
        language = WebSearcherLanguage.ENGLISH;
    }

    @Override
    public List<String> searchUrls(String query) {
        List<String> urls = new ArrayList<String>();

        List<R> webresults = search(query);
        for (R webresult : webresults) {
            urls.add(webresult.getUrl());
        }

        return urls;
    }

    @Override
    public int getTotalResultCount(String query) {
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
