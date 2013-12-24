package ws.palladian.retrieval.search;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;

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

public abstract class BaseTopsySearcher extends AbstractSearcher<WebContent> {

    /** The identifier for the API key when provided via {@link Configuration}. */
    public static final String CONFIG_API_KEY = "api.topsy.key";

    private final String apiKey;

    private final HttpRetriever retriever;

    /**
     * <p>
     * Create a new Topsy searcher with the specified API key.
     * </p>
     * 
     * @param apiKey The API key, not <code>null</code> or empty.
     */
    protected BaseTopsySearcher(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
        this.retriever = HttpRetrieverFactory.getHttpRetriever();
    }

    /**
     * <p>
     * Create a new Topsy searcher with an API key provided by a {@link Configuration} instance.
     * </p>
     * 
     * @param configuration The Configuration providing the required API key via key {@value #CONFIG_API_KEY}, not
     *            <code>null</code>.
     */
    protected BaseTopsySearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    @Override
    public List<WebContent> search(String query, int resultCount, Language language) throws SearcherException {
        List<WebContent> result = CollectionHelper.newArrayList();
        // # of necessary requests, we fetch in chunks of 100
        int numRequests = (int)Math.ceil(resultCount / 100.);
        for (int page = 1; page <= Math.min(numRequests, 10); page++) {
            String queryUrl = buildQueryUrl(query, page, apiKey);
            HttpResult httpResult;
            try {
                httpResult = retriever.httpGet(queryUrl);
            } catch (HttpException e) {
                throw new SearcherException("HTTP error while searching with URL \"" + query + "\": " + e.getMessage(),
                        e);
            }
//            checkRateLimit(httpResult);
            String jsonString = httpResult.getStringContent();
            try {
                JsonObject jsonResult = new JsonObject(jsonString);
                JsonObject responseJson = jsonResult.getJsonObject("response");
                JsonArray listJson = responseJson.getJsonArray("list");
                for (int i = 0; i < listJson.size(); i++) {
                    JsonObject item = listJson.getJsonObject(i);
                    WebContent webResult = parse(item);
                    result.add(webResult);
                    if (result.size() == resultCount) {
                        break;
                    }
                }
            } catch (JsonException e) {
                throw new SearcherException("Error parsing the JSON response " + e.getMessage() + ", JSON was \""
                        + jsonString + "\"", e);
            }
        }
        return result;
    }

    // this information is not provided, although it is mentioned here:
    // https://code.google.com/p/otterapi/wiki/RateLimit
//    private void checkRateLimit(HttpResult httpResult) throws RateLimitedException {
//        int limit = Integer.valueOf(httpResult.getHeaderString("X-RateLimit-Limit"));
//        int remaining = Integer.valueOf(httpResult.getHeaderString("X-RateLimit-Remaining"));
//        int reset = Integer.valueOf(httpResult.getHeaderString("X-RateLimit-Reset"));
//        if (remaining == 0) {
//            int timeUntilReset = reset - (int)(System.currentTimeMillis() / 1000);
//            throw new RateLimitedException("Rate limit exceeded, allowed " + limit, timeUntilReset);
//        }
//    }

    /**
     * Subclass provides the URL for the query.
     */
    protected abstract String buildQueryUrl(String query, int page, String apiKey);

    /**
     * Subclass performs the parsing for each item in the JSON list.
     */
    protected abstract WebContent parse(JsonObject item) throws JsonException;

}
