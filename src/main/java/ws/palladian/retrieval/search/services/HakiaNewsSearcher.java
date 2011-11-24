package ws.palladian.retrieval.search.services;

import ws.palladian.retrieval.search.Searcher;
import ws.palladian.retrieval.search.WebResult;

public final class HakiaNewsSearcher extends BaseHakiaSearcher implements Searcher<WebResult> {

    public HakiaNewsSearcher(String apiKey) {
        super(apiKey);
    }
    
    public HakiaNewsSearcher() {
        super();
    }

    @Override
    protected String getEndpoint() {
        return "http://syndication.hakia.com/searchapi.aspx?search.type=news";
    }

    @Override
    public String getName() {
        return "Hakia News";
    }

}
