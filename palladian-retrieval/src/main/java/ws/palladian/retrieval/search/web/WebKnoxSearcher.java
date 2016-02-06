package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;
import ws.palladian.retrieval.search.AbstractMultifacetSearcher;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearchResults;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * {@link AbstractWebKnoxSearcher} implementation for WebKnox.
 * 
 * @see <a href="https://webknox.com/">WebKnox</a>
 * @author David Urbansky
 */
public final class WebKnoxSearcher extends AbstractMultifacetSearcher<WebContent> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WebKnoxSearcher.class);

    /** The base URL endpoint of the WebKnox service. */
    protected static final String BASE_SERVICE_URL = "https://webknox.com:8443/";

    /** Key of the {@link Configuration} key for the API key. */
    public static final String CONFIG_API_KEY = "api.webknox.apiKey";

    protected final String apiKey;

    private final HttpRetriever retriever;

    /**
     * <p>
     * Creates a new WebKnox searcher.
     * </p>
     * 
     * @param apiKey The api key for accessing WebKnox.
     */
    public WebKnoxSearcher(String apiKey) {
        Validate.notEmpty(apiKey, "api key must not be empty");
        this.apiKey = apiKey;
        this.retriever = HttpRetrieverFactory.getHttpRetriever();
    }

    /**
     * <p>
     * Creates a new WebKnox searcher.
     * </p>
     * 
     * @param configuration The configuration which must provide an account key for accessing WebKnox, which must be
     *            provided as string via key <tt>api.webknox.apiKey</tt> in the configuration.
     */
    public WebKnoxSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    @Override
    public SearchResults<WebContent> search(MultifacetQuery query) throws SearcherException {

        List<WebContent> webResults = new ArrayList<>();

        try {
            String requestUrl = buildRequestUrl(query.getText(), 0, query.getResultCount(), query.getLanguage());
            LOGGER.debug("URL = {}", requestUrl);

            HttpResult httpResult = retriever.httpGet(requestUrl);
            if (httpResult.errorStatus()) {
                throw new SearcherException("Encountered HTTP status code " + httpResult.getStatusCode()
                        + " when accessing " + requestUrl + ".");
            }
            String jsonString = httpResult.getStringContent();
            LOGGER.debug("JSON = {}", jsonString);

            JsonArray jsonResults = new JsonArray(jsonString);

            for (int j = 0; j < jsonResults.size(); j++) {
                JsonObject currentResult = jsonResults.getJsonObject(j);
                WebContent webResult = parseResult(currentResult);
                webResults.add(webResult);
                if (webResults.size() >= query.getResultCount()) {
                    break;
                }
            }

            return new SearchResults<>(webResults, (long)jsonResults.size());

        } catch (HttpException e) {
            throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName() + ": "
                    + e.getMessage(), e);
        } catch (JsonException e) {
            throw new SearcherException("Error parsing the JSON response while searching for \"" + query + "\" with "
                    + getName() + ": " + e.getMessage(), e);
        }

    }

    protected String buildRequestUrl(String query, int offset, int count, Language language) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(BASE_SERVICE_URL).append("webpage/search");
        urlBuilder.append("?query=").append(UrlHelper.encodeParameter(query));
        urlBuilder.append("&language=").append(language.getIso6391());
        urlBuilder.append("&number=").append(Math.min(count, 50));
        urlBuilder.append("&apiKey=").append(apiKey);
        return urlBuilder.toString();
    }

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
