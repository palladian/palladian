package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;

/**
 * <p>
 * Search for Hakia news.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class HakiaNewsSearcher extends BaseHakiaSearcher {

    /**
     * @see BaseHakiaSearcher#BaseHakiaSearcher(String)
     */
    public HakiaNewsSearcher(String apiKey) {
        super(apiKey);
    }

    /**
     * @see BaseHakiaSearcher#BaseHakiaSearcher(Configuration)
     */
    public HakiaNewsSearcher(Configuration configuration) {
        super(configuration);
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
