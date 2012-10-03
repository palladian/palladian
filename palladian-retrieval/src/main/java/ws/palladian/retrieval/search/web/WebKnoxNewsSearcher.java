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

    /**
     * @see BaseWebKnoxSearcher#BaseWebKnoxSearcher(String)
     */
    public WebKnoxNewsSearcher(String appId, String apiKey) {
        super(appId, apiKey);
    }

    /**
     * @see BaseWebKnoxSearcher#BaseWebKnoxSearcher(Configuration)
     */
    public WebKnoxNewsSearcher(Configuration configuration) {
        super(configuration);
    }

    @Override
    protected String buildRequestUrl(String query, Language language, int offset, int count) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(BASE_SERVICE_URL).append("news/search");
        urlBuilder.append("?query=").append(UrlHelper.urlEncode(query));
        urlBuilder.append("&offset=").append(offset);
        urlBuilder.append("&numResults=").append(Math.min(count, 100));
        urlBuilder.append("&apiKey=").append(apiKey);
        urlBuilder.append("&appId=").append(appId);

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

    // public static void main(String[] args) throws SearcherException {
    // WebKnoxNewsSearcher webKnoxSearcher = new WebKnoxNewsSearcher(ConfigHolder.getInstance().getConfig());
    // CollectionHelper.print(webKnoxSearcher.search("Nokia Lumia 920", 10));
    // }
}
