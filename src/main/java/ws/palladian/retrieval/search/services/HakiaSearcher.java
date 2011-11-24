package ws.palladian.retrieval.search.services;

import ws.palladian.retrieval.search.Searcher;
import ws.palladian.retrieval.search.WebResult;

public final class HakiaSearcher extends BaseHakiaSearcher implements Searcher<WebResult> {

    public HakiaSearcher(String apiKey) {
        super(apiKey);
    }
    
    public HakiaSearcher() {
        super();
    }

    @Override
    protected String getEndpoint() {
        return "http://syndication.hakia.com/searchapi.aspx?search.type=search";
    }

    @Override
    public String getName() {
        return "Hakia";
    }

}
