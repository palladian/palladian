package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.PropertiesConfiguration;

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
     * @see BaseHakiaSearcher#BaseHakiaSearcher(PropertiesConfiguration)
     */
    public HakiaNewsSearcher(PropertiesConfiguration configuration) {
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
