package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;

import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.BaseTopsySearcher;

/**
 * <p>
 * Search for Tweets containing a specified URL on Topsy.
 * </p>
 * 
 * @author Philipp KAtz
 */
@Deprecated
public final class TopsyUrlSearcher extends BaseTopsySearcher {

    private static final String SEARCHER_NAME = "Topsy Links";

    /**
     * @see BaseTopsySearcher#BaseTopsySearcher(String)
     */
    public TopsyUrlSearcher(String apiKey) {
        super(apiKey);
    }

    /**
     * <p>
     * Create a new Topsy URL searcher with an API key provided by a {@link Configuration} instance.
     * </p>
     * 
     * @param configuration The Configuration providing the required API key via key {@value #CONFIG_API_KEY}, not
     *            <code>null</code>.
     */
    public TopsyUrlSearcher(Configuration configuration) {
        super(configuration);
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    @Override
    protected String buildQueryUrl(String query, int page, String apiKey) {
        if (!query.startsWith("http://") && !query.startsWith("https://")) {
            throw new IllegalArgumentException("Invalid parameter, only URLs are supported (was: \"" + query + "\")");
        }

        StringBuilder queryUrl = new StringBuilder();
        queryUrl.append("http://otter.topsy.com/trackbacks.json");
        queryUrl.append("?url=").append(query);
        queryUrl.append("&apikey=").append(apiKey);
        queryUrl.append("&type=tweet"); // XXX more types are supported
        queryUrl.append("&page=").append(page);
        queryUrl.append("&perpage=100");
        queryUrl.append("&allow_lang=en");
        return queryUrl.toString();
    }

    @Override
    protected WebContent parse(JsonObject item) throws JsonException {
        BasicWebContent.Builder builder = new BasicWebContent.Builder();
        builder.setUrl(item.tryGetString("permalink_url"));
        builder.setTitle(item.tryGetString("content"));
        return builder.create();
    }

}
