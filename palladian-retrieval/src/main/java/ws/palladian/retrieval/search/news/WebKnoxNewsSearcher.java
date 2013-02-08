package ws.palladian.retrieval.search.news;

import java.sql.Date;

import org.apache.commons.configuration.Configuration;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.search.BaseWebKnoxSearcher;
import ws.palladian.retrieval.search.web.WebResult;
import ws.palladian.retrieval.search.web.WebSearcher;

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
public class WebKnoxNewsSearcher extends BaseWebKnoxSearcher<WebResult> {

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
    protected String buildRequestUrl(String query, Language language, int offset, int count) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(BASE_SERVICE_URL).append("news/search");
        urlBuilder.append("?query=").append(UrlHelper.encodeParameter(query));
        urlBuilder.append("&offset=").append(offset);
        urlBuilder.append("&numResults=").append(Math.min(count, 100));
        urlBuilder.append("&exactTitleMatch=").append(onlyExactMatchesInTitle);
        urlBuilder.append("&apiKey=").append(apiKey);

        // System.out.println(urlBuilder);

        return urlBuilder.toString();
    }

    @Override
    protected WebResult parseResult(JSONObject currentResult) throws JSONException {
        String url = currentResult.getString("url");
        String title = currentResult.getString("title");
        String summary = currentResult.getString("summary");
        Date date = null;
        String publishTimestamp = currentResult.getString("timestamp");
        if (!publishTimestamp.isEmpty()) {
            try {
                date = new Date(Long.valueOf(publishTimestamp) * 1000);
            } catch (Exception e) {
            }
        }
        WebResult webResult = new WebResult(url, title, summary, date, getName());

        return webResult;
    }

    @Override
    public String getName() {
        return "WebKnox News";
    }

    // public boolean isOnlyExactMatchesInTitle() {
    // return onlyExactMatchesInTitle;
    // }
    //
    // public void setOnlyExactMatchesInTitle(boolean onlyExactMatchesInTitle) {
    // this.onlyExactMatchesInTitle = onlyExactMatchesInTitle;
    // }

    // public static void main(String[] args) throws SearcherException {
    // WebKnoxNewsSearcher webKnoxSearcher = new WebKnoxNewsSearcher(ConfigHolder.getInstance().getConfig(), true);
    // CollectionHelper.print(webKnoxSearcher.search("Apple iPhone 5", 10));
    // }

}
