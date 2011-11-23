package ws.palladian.retrieval.search.services;

public final class GoogleNewsSearcher extends BaseGoogleSearcher implements WebSearcher {

    @Override
    protected String getBaseUrl() {
        return "http://ajax.googleapis.com/ajax/services/search/news";
    }

    @Override
    public String getName() {
        return "Google News";
    }

}
