package ws.palladian.retrieval.search;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.search.web.WebResult;
import ws.palladian.retrieval.search.web.WebSearcher;

/**
 * <p>
 * Base implementation for WebKnox searchers.
 * </p>
 * 
 * @see <a href="http://webknox.com/api">WebKnox API</a>
 * @author David Urbansky
 */
public abstract class BaseWebKnoxSearcher<R extends WebResult> extends WebSearcher<R> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseWebKnoxSearcher.class);

    /** The base URL endpoint of the WebKnox service. */
    protected static final String BASE_SERVICE_URL = "http://webknox.com/api/";

    /** Key of the {@link Configuration} key for the API key. */
    public static final String CONFIG_API_KEY = "api.webknox.apiKey";

    protected final String apiKey;

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

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

    /**
     * <p>
     * Only English is supported.
     * </p>
     */
    @Override
    public List<R> search(String query, int resultCount, Language language) throws SearcherException {

        List<R> webResults = new ArrayList<R>();

        try {
            String requestUrl = buildRequestUrl(query, language, 0, resultCount);
            HttpResult httpResult = retriever.httpGet(requestUrl);
            TOTAL_REQUEST_COUNT.incrementAndGet();

            String jsonString = HttpHelper.getStringContent(httpResult);
            JSONObject jsonObject = new JSONObject(jsonString);

            if (!jsonObject.has("results")) {
                return webResults;
            }

            JSONArray jsonResults = jsonObject.getJSONArray("results");

            for (int j = 0; j < jsonResults.length(); j++) {
                JSONObject currentResult = jsonResults.getJSONObject(j);
                R webResult = parseResult(currentResult);
                webResults.add(webResult);

                if (webResults.size() >= resultCount) {
                    break;
                }
            }

        } catch (HttpException e) {
            throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName() + ": "
                    + e.getMessage(), e);
        } catch (JSONException e) {
            throw new SearcherException("Error parsing the JSON response while searching for \"" + query + "\" with "
                    + getName() + ": " + e.getMessage(), e);
        }

        LOGGER.debug("webknox requests: " + TOTAL_REQUEST_COUNT.get());

        return webResults;
    }

    /**
     * <p>
     * Parse the {@link JSONObject} to the desired type of {@link WebResult}.
     * </p>
     * 
     * @param currentResult
     * @return
     * @throws JSONException
     */
    protected abstract R parseResult(JSONObject currentResult) throws JSONException;

    /**
     * <p>
     * Build a search request URL based on the supplied parameters.
     * </p>
     * 
     * @param query the raw query, no escaping necessary.
     * @param language the language for which to search, may be <code>null</code>.
     * @param offset the paging offset, 0 for no offset.
     * @param count the number of results to retrieve.
     * @return
     */
    protected abstract String buildRequestUrl(String query, Language language, int offset, int count);

    @Override
    public long getTotalResultCount(String query, Language language) throws SearcherException {
        throw new SearcherException("Getting the total result count is not supported in the new WebKnox API.");
    }

    /**
     * Gets the number of HTTP requests sent to WebKnox.
     * 
     * @return
     */
    public static int getRequestCount() {
        return TOTAL_REQUEST_COUNT.get();
    }

}
