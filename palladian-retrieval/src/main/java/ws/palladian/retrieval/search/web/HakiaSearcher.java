package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;

import ws.palladian.retrieval.search.BaseHakiaSearcher;

/**
 * <p>
 * Search for Hakia.
 * </p>
 * 
 * @author Philipp Katz
 * @deprecated Not really reliable (any longer).
 */
@Deprecated
public final class HakiaSearcher extends BaseHakiaSearcher {

    /**
     * @see BaseHakiaSearcher#BaseHakiaSearcher(String)
     */
    public HakiaSearcher(String apiKey) {
        super(apiKey);
    }

    /**
     * @see BaseHakiaSearcher#BaseHakiaSearcher(Configuration)
     */
    public HakiaSearcher(Configuration configuration) {
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
