package ws.palladian.retrieval.search.services;

public final class HakiaNewsSearcher extends BaseHakiaSearcher {

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
