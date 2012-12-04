package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;

/**
 * <p>
 * {@link WebSearcher} implementation for webknox.
 * </p>
 * 
 * 
 * @see http://www.webknox.com/
 * @see http://webknox.com/api#!/index/search_GET
 * @author David Urbansky
 */
public class WebKnoxSearcher extends BaseWebKnoxSearcher<WebResult> {

    /**
     * @see BaseWebKnoxSearcher#BaseWebKnoxSearcher(String)
     */
    public WebKnoxSearcher(String apiKey) {
        super(apiKey);
    }

    /**
     * @see BaseWebKnoxSearcher#BaseWebKnoxSearcher(Configuration)
     */
    public WebKnoxSearcher(Configuration configuration) {
        super(configuration);
    }

    @Override
    protected String buildRequestUrl(String query, Language language, int offset, int count) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(BASE_SERVICE_URL).append("index/websites");
        urlBuilder.append("?query=").append(UrlHelper.urlEncode(query));
        urlBuilder.append("&offset=").append(offset);
        urlBuilder.append("&numResults=").append(Math.min(count, 100));
        urlBuilder.append("&apiKey=").append(apiKey);

        // System.out.println(urlBuilder);

        return urlBuilder.toString();
    }

    @Override
    protected WebResult parseResult(JSONObject currentResult) throws JSONException {
        String summary = null;
        String url = currentResult.getString("url");
        String title = currentResult.getString("title");
        WebResult webResult = new WebResult(url, title, summary, getName());

        return webResult;
    }

    @Override
    public String getName() {
        return "WebKnox";
    }

    // public static void main(String[] args) throws SearcherException {
    // WebKnoxSearcher webKnoxSearcher = new WebKnoxSearcher(ConfigHolder.getInstance().getConfig());
    // CollectionHelper.print(webKnoxSearcher.search("Conan O'Brien", 10));
    // }

}
