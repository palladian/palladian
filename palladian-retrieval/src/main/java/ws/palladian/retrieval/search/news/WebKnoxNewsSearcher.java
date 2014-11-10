package ws.palladian.retrieval.search.news;

import java.sql.Date;

import org.apache.commons.configuration.Configuration;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.BaseWebKnoxSearcher;

/**
 * <p>
 * {@link WebSearcher} implementation for webknox news.
 * </p>
 * 
 * 
 * @see http://www.webknox.com/
 * @see http://webknox.com/api#!/news/search_GET
 * @author David Urbansky
 */
public final class WebKnoxNewsSearcher extends BaseWebKnoxSearcher {

    /** If true, only news are returned, that contain the search term exactly as given in their titles. */
    private final boolean onlyExactMatchesInTitle;

    /**
     * @param apiKey The API key.
     * @param onlyExactMatchesInTitle If true, only news are returned, that contain the search term exactly as given in
     *            their titles.
     * 
     * @see BaseWebKnoxSearcher#BaseWebKnoxSearcher(String)
     */
    public WebKnoxNewsSearcher(String apiKey, boolean onlyExactMatchesInTitle) {
        super(apiKey);
        this.onlyExactMatchesInTitle = onlyExactMatchesInTitle;
    }

    /**
     * @param configuration A {@link Configuration} instance providing the required API key as
     *            {@value BaseWebKnoxSearcher#CONFIG_API_KEY}, not <code>null</code>.
     * @param onlyExactMatchesInTitle If true, only news are returned, that contain the search term exactly as given in
     *            their titles.
     * @see BaseWebKnoxSearcher#BaseWebKnoxSearcher(Configuration)
     */
    public WebKnoxNewsSearcher(Configuration configuration, boolean onlyExactMatchesInTitle) {
        super(configuration);
        this.onlyExactMatchesInTitle = onlyExactMatchesInTitle;
    }

    /**
     * <p>
     * Create a new {@link WebKnoxNewsSearcher} with onlyExactMatchesInTitle set to <code>false</code>.
     * </p>
     * 
     * @param apiKey The API key.
     */
    public WebKnoxNewsSearcher(String apiKey) {
        this(apiKey, false);
    }

    /**
     * <p>
     * Create a new {@link WebKnoxNewsSearcher} with onlyExactMatchesInTitle set to <code>false</code>.
     * </p>
     * 
     * @param configuration A {@link Configuration} instance providing the required API key as
     *            {@value BaseWebKnoxSearcher#CONFIG_API_KEY}, not <code>null</code>.
     */
    public WebKnoxNewsSearcher(Configuration configuration) {
        this(configuration, false);
    }

    @Override
    protected String buildRequestUrl(String query, int offset, int count) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(BASE_SERVICE_URL).append("news/search");
        urlBuilder.append("?query=").append(UrlHelper.encodeParameter(query));
        urlBuilder.append("&offset=").append(offset);
        urlBuilder.append("&numResults=").append(Math.min(count, 100));
        urlBuilder.append("&exactTitleMatch=").append(onlyExactMatchesInTitle);
        urlBuilder.append("&apiKey=").append(apiKey);
        return urlBuilder.toString();
    }

    @Override
    protected WebContent parseResult(JsonObject currentResult) throws JsonException {
        BasicWebContent.Builder builder = new BasicWebContent.Builder();
        builder.setUrl(currentResult.getString("url"));
        builder.setTitle(currentResult.getString("title"));
        builder.setSummary(currentResult.getString("summary"));
        String publishTimestamp = currentResult.getString("timestamp");
        if (!publishTimestamp.isEmpty()) {
            try {
                builder.setPublished(new Date(Long.parseLong(publishTimestamp) * 1000));
            } catch (Exception e) {
            }
        }
        return builder.create();
    }

    @Override
    public String getName() {
        return "WebKnox News";
    }

}
