package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * <p>
 * Search for Hakia.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class HakiaSearcher extends BaseHakiaSearcher {

    /**
     * @see BaseHakiaSearcher#BaseHakiaSearcher(String)
     */
    public HakiaSearcher(String apiKey) {
        super(apiKey);
    }

    /**
     * @see BaseHakiaSearcher#BaseHakiaSearcher(PropertiesConfiguration)
     */
    public HakiaSearcher(PropertiesConfiguration configuration) {
        super(configuration);
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
