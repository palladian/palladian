package ws.palladian.retrieval.search.services;

import ws.palladian.retrieval.search.WebSearcher;

public final class GoogleSearcher extends BaseGoogleSearcher implements WebSearcher {

    @Override
    protected String getBaseUrl() {
        return "http://ajax.googleapis.com/ajax/services/search/web";
    }

    @Override
    public String getName() {
        return "Google";
    }

}
