package ws.palladian.retrieval.search.services;

import ws.palladian.retrieval.search.WebSearcher;

public final class HakiaNewsSearcher extends BaseHakiaSearcher implements WebSearcher {

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
