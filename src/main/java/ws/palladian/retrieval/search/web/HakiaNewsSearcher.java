package ws.palladian.retrieval.search.web;

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
