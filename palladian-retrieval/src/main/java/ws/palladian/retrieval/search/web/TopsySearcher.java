package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.parser.JsonHelper;
import ws.palladian.retrieval.search.BaseTopsySearcher;
import ws.palladian.retrieval.search.WebContent;

/**
 * <p>
 * Search for Tweets on <a href="http://topsy.com">Topsy</a>. Topsy has a better archive, so we can search older Tweets.
 * </p>
 * 
 * @author Philipp Katz
 * @see <a href="http://code.google.com/p/otterapi/">Topsy REST API</a>
 * @see <a href="http://manage.topsy.com/">Topsy API registration</a>
 */
public final class TopsySearcher extends BaseTopsySearcher {

    private static final String SEARCHER_NAME = "Topsy";

    /**
     * @see BaseTopsySearcher#BaseTopsySearcher(String)
     */
    public TopsySearcher(String apiKey) {
        super(apiKey);
    }

    /**
     * <p>
     * Create a new Topsy searcher with an API key provided by a {@link Configuration} instance.
     * </p>
     * 
     * @param configuration The Configuration providing the required API key via key {@value #CONFIG_API_KEY}, not
     *            <code>null</code>.
     */
    public TopsySearcher(Configuration configuration) {
        super(configuration);
    }

    @Override
    public String getName() {
        return SEARCHER_NAME;
    }

    protected WebContent parse(JSONObject item) throws JSONException {
        String url = JsonHelper.getString(item, "trackback_permalink");
        String title = StringEscapeUtils.unescapeHtml4(JsonHelper.getString(item, "title"));
        // String description = StringEscapeUtils.unescapeHtml4(JsonHelper.getString(item, "content"));
        return new BasicWebContent(url, title, null);
    }

    protected String buildQueryUrl(String query, int page, String apiKey) {
        StringBuilder queryUrl = new StringBuilder();
        queryUrl.append("http://otter.topsy.com/search.json");
        queryUrl.append("?q=").append(UrlHelper.encodeParameter(query));
        queryUrl.append("&window=dynamic");
        queryUrl.append("&apikey=").append(apiKey);
        queryUrl.append("&type=tweet"); // XXX more types are supported
        queryUrl.append("&page=").append(page);
        queryUrl.append("&perpage=100");
        queryUrl.append("&allow_lang=en");
        return queryUrl.toString();
    }

}
