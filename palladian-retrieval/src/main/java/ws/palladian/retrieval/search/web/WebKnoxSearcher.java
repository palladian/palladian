package ws.palladian.retrieval.search.web;

import org.apache.commons.configuration.Configuration;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractWebKnoxSearcher;

/**
 * <p>
 * {@link AbstractWebKnoxSearcher} implementation for webknox.
 * </p>
 * 
 * 
 * @see https://webknox.com/
 * @see https://webknox.com/api#!/index/search_GET
 * @author David Urbansky
 */
public final class WebKnoxSearcher extends AbstractWebKnoxSearcher {

    /**
     * @see AbstractWebKnoxSearcher#AbstractWebKnoxSearcher(String)
     */
    public WebKnoxSearcher(String apiKey) {
        super(apiKey);
    }

    /**
     * @see AbstractWebKnoxSearcher#AbstractWebKnoxSearcher(Configuration)
     */
    public WebKnoxSearcher(Configuration configuration) {
        super(configuration);
    }

    @Override
    protected String buildRequestUrl(String query, int offset, int count, Language language) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(BASE_SERVICE_URL).append("webpage/search");
        urlBuilder.append("?query=").append(UrlHelper.encodeParameter(query));
        urlBuilder.append("&language=").append(language.getIso6391());
        urlBuilder.append("&number=").append(Math.min(count, 50));
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
