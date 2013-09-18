package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.BaseWebKnoxSearcher;

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
public class WebKnoxSearcher extends BaseWebKnoxSearcher<WebContent> {

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
        urlBuilder.append("?query=").append(UrlHelper.encodeParameter(query));
        urlBuilder.append("&offset=").append(offset);
        urlBuilder.append("&numResults=").append(Math.min(count, 100));
        urlBuilder.append("&apiKey=").append(apiKey);

        // System.out.println(urlBuilder);

        return urlBuilder.toString();
    }

    @Override
    protected WebContent parseResult(JSONObject currentResult) throws JSONException {
        String summary = null;
        String url = currentResult.getString("url");
        String title = currentResult.getString("title");
        return new BasicWebContent(url, title, summary);
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
