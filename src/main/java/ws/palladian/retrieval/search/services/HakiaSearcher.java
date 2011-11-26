package ws.palladian.retrieval.search.services;

public final class HakiaSearcher extends BaseHakiaSearcher {

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
