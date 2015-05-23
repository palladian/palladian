package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
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
public final class WebKnoxSearcher extends BaseWebKnoxSearcher {

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
    protected String buildRequestUrl(String query, int offset, int count) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(BASE_SERVICE_URL).append("index/websites");
        urlBuilder.append("?query=").append(UrlHelper.encodeParameter(query));
        urlBuilder.append("&offset=").append(offset);
        urlBuilder.append("&numResults=").append(Math.min(count, 100));
        urlBuilder.append("&apiKey=").append(apiKey);
        return urlBuilder.toString();
    }

    @Override
    protected WebContent parseResult(JsonObject currentResult) throws JsonException {
        BasicWebContent.Builder builder = new BasicWebContent.Builder();
        builder.setUrl(currentResult.getString("url"));
        builder.setTitle(currentResult.getString("title"));
        return builder.create();
    }

    @Override
    public String getName() {
        return "WebKnox";
    }

}
