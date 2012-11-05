package ws.palladian.retrieval.search.web;

import java.sql.Date;

import org.apache.commons.configuration.Configuration;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;

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
    private boolean onlyExactMatchesInTitle = false;

    /**
     * @param apiKey The API key.
     * @param onlyExactMatchesInTitle If true, only news are returned, that contain the search term exactly as given in
     *            their titles.
     * 
     * @see BaseWebKnoxSearcher#BaseWebKnoxSearcher(String)
     */
    public WebKnoxNewsSearcher(String apiKey, boolean onlyExactMatchesInTitle) {
        super(apiKey);
        setOnlyExactMatchesInTitle(onlyExactMatchesInTitle);
    }

    /**
     * @param onlyExactMatchesInTitle If true, only news are returned, that contain the search term exactly as given in
     *            their titles.
     * @see BaseWebKnoxSearcher#BaseWebKnoxSearcher(Configuration)
     */
    public WebKnoxNewsSearcher(Configuration configuration, boolean onlyExactMatchesInTitle) {
        super(configuration);
        setOnlyExactMatchesInTitle(onlyExactMatchesInTitle);
    }

    @Override
    protected String buildRequestUrl(String query, Language language, int offset, int count) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(BASE_SERVICE_URL).append("news/search");
        urlBuilder.append("?query=").append(UrlHelper.urlEncode(query));
        urlBuilder.append("&offset=").append(offset);
        urlBuilder.append("&numResults=").append(Math.min(count, 100));

        if (isOnlyExactMatchesInTitle()) {
            urlBuilder.append("&exactTitleMatch=true");
        } else {
            urlBuilder.append("&exactTitleMatch=false");
        }

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
                date = new Date(Long.valueOf(publishTimestamp));
            } catch(Exception e) {}
        }
        WebResult webResult = new WebResult(url, title, summary, date, getName());

        return webResult;
    }

    @Override
    public String getName() {
        return "WebKnox News";
    }

    public boolean isOnlyExactMatchesInTitle() {
        return onlyExactMatchesInTitle;
    }

    public void setOnlyExactMatchesInTitle(boolean onlyExactMatchesInTitle) {
        this.onlyExactMatchesInTitle = onlyExactMatchesInTitle;
    }

    // public static void main(String[] args) throws SearcherException {
    // WebKnoxNewsSearcher webKnoxSearcher = new WebKnoxNewsSearcher(ConfigHolder.getInstance().getConfig(), true);
    // CollectionHelper.print(webKnoxSearcher.search("Nokia Lumia 920", 10));
    // }

}
