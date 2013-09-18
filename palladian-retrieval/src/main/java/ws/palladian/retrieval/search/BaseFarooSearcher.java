package ws.palladian.retrieval.search;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;

/**
 * <p>
 * {@link WebSearcher} implementation for faroo. Rate limit is 100,000 queries per month.
 * </p>
 * 
 * @see <a href="http://www.faroo.com/">FAROO Peer-to-peer Web Search</a>
 * @see <a href="http://www.faroo.com/hp/api/api.html#jsonp">API doc.</a>
 * @author David Urbansky
 */
public abstract class BaseFarooSearcher extends AbstractSearcher<WebContent> {

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    /** Key of the {@link Configuration} key for the account key. */
    public static final String CONFIG_ACCOUNT_KEY = "api.faroo.key";

    protected final String key;
    
    private final HttpRetriever retriever;

    /**
     * <p>
     * Creates a new Faroo searcher.
     * </p>
     * 
     * @param accountKey The account key for accessing Faroo.
     */
    public BaseFarooSearcher(String key) {
        Validate.notEmpty(key, "key must not be empty");
        this.key = key;
        this.retriever = HttpRetrieverFactory.getHttpRetriever();
    }

    /**
     * <p>
     * Creates a new Faroo searcher.
     * </p>
     * 
     * @param configuration The configuration which must provide an account key for accessing Faroo, which must be
     *            provided as string via key <tt>api.faroo.key</tt> in the configuration.
     */
    public BaseFarooSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_ACCOUNT_KEY));
    }

    /**
     * <p>
     * Supported languages are English, German, and Chinese.
     * </p>
     */
    @Override
    public List<WebContent> search(String query, int resultCount, Language language) throws SearcherException {

        List<WebContent> webResults = new ArrayList<WebContent>();
        HttpResult httpResult;

        try {
            String requestUrl = getRequestUrl(query, resultCount, language);
            httpResult = retriever.httpGet(requestUrl);
            TOTAL_REQUEST_COUNT.incrementAndGet();

        } catch (HttpException e) {
            throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName() + ": "
                    + e.getMessage(), e);
        }

        String jsonString = httpResult.getStringContent();

        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            if (!jsonObject.has("results")) {
                return webResults;
            }

            JSONArray jsonResults = jsonObject.getJSONArray("results");

            for (int j = 0; j < jsonResults.length(); j++) {
                JSONObject jsonResult = jsonResults.getJSONObject(j);
                String summary = null;
                if (jsonResult.has("kwic")) {
                    summary = jsonResult.getString("kwic");
                }
                String url = jsonResult.getString("url");
                String title = jsonResult.getString("title");
                BasicWebContent webResult = new BasicWebContent(url, title, summary);
                webResults.add(webResult);
                if (webResults.size() >= resultCount) {
                    break;
                }
            }

        } catch (JSONException e) {
            throw new SearcherException("Error parsing the JSON response while searching for \"" + query + "\" with "
                    + getName() + ": " + e.getMessage() + ", JSON \"" + jsonString + "\"", e);
        }

        return webResults;
    }

    protected abstract String getRequestUrl(String query, int resultCount, Language language);

    /**
     * <p>
     * Gets the number of HTTP requests sent to faroo.
     * </p>
     * 
     * @return
     */
    public static int getRequestCount() {
        return TOTAL_REQUEST_COUNT.intValue();
    }

}
