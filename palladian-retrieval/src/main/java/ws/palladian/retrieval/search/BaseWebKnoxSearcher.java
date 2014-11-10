package ws.palladian.retrieval.search;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.WebContent;

/**
 * <p>
 * Base implementation for WebKnox searchers.
 * </p>
 * 
 * @see <a href="http://webknox.com/api">WebKnox API</a>
 * @author David Urbansky
 */
public abstract class BaseWebKnoxSearcher extends AbstractMultifacetSearcher<WebContent> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseWebKnoxSearcher.class);

    /** The base URL endpoint of the WebKnox service. */
    protected static final String BASE_SERVICE_URL = "http://webknox.com/api/";

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
    public BaseWebKnoxSearcher(String apiKey) {
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
    public BaseWebKnoxSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    @Override
    public SearchResults<WebContent> search(MultifacetQuery query) throws SearcherException {

        if (query.getLanguage() != null && query.getLanguage() != Language.ENGLISH) {
            throw new SearcherException("Only English langauge is supported by " + getName() + ".");
        }

        List<WebContent> webResults = CollectionHelper.newArrayList();

        try {
            String requestUrl = buildRequestUrl(query.getText(), 0, query.getResultCount());
            LOGGER.debug("URL = {}", requestUrl);

            HttpResult httpResult = retriever.httpGet(requestUrl);
            if (httpResult.errorStatus()) {
                throw new SearcherException("Encountered HTTP status code " + httpResult.getStatusCode()
                        + " when accessing " + requestUrl + ".");
            }
            String jsonString = httpResult.getStringContent();
            LOGGER.debug("JSON = {}", jsonString);

            JsonObject jsonObject = new JsonObject(jsonString);
            JsonArray jsonResults = jsonObject.getJsonArray("results");
            long numResults = jsonObject.getLong("totalResults");

            for (int j = 0; j < jsonResults.size(); j++) {
                JsonObject currentResult = jsonResults.getJsonObject(j);
                WebContent webResult = parseResult(currentResult);
                webResults.add(webResult);
                if (webResults.size() >= query.getResultCount()) {
                    break;
                }
            }

            return new SearchResults<WebContent>(webResults, numResults);

        } catch (HttpException e) {
            throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName() + ": "
                    + e.getMessage(), e);
        } catch (JsonException e) {
            throw new SearcherException("Error parsing the JSON response while searching for \"" + query + "\" with "
                    + getName() + ": " + e.getMessage(), e);
        }

    }

    /**
     * <p>
     * Parse the {@link JSONObject} to an instance of {@link WebContent}.
     * </p>
     * 
     * @param currentResult The current JSON item.
     * @return A parsed WebContent.
     * @throws JSONException In case, parsing fails.
     */
    protected abstract WebContent parseResult(JsonObject currentResult) throws JsonException;

    /**
     * <p>
     * Build a search request URL based on the supplied parameters.
     * </p>
     * 
     * @param query the raw query, no escaping necessary.
     * @param offset the paging offset, 0 for no offset.
     * @param count the number of results to retrieve.
     * @return
     */
    protected abstract String buildRequestUrl(String query, int offset, int count);

}
