package ws.palladian.retrieval.search;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.FixedIntervalRequestThrottle;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
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

    /** Faroo allows 1 query/second. */
    private static final RequestThrottle THROTTLE = new FixedIntervalRequestThrottle(1, TimeUnit.SECONDS);

    private final String key;

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
            THROTTLE.hold();
            httpResult = retriever.httpGet(requestUrl);
            if (httpResult.getStatusCode() == 401) {
                throw new SearcherException("Encountered HTTP status 401, API key is not accepted.");
            } else if (httpResult.getStatusCode() == 429) {
                throw new RateLimitedException("Encountered HTTP status 429, rate limit is exceeded.");
            }
            TOTAL_REQUEST_COUNT.incrementAndGet();

        } catch (HttpException e) {
            throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName() + ": "
                    + e.getMessage(), e);
        }

        String jsonString = httpResult.getStringContent();

        try {
            JsonObject jsonObject = new JsonObject(jsonString);
            if (jsonObject.get("results") == null) {
                return webResults;
            }

            JsonArray jsonResults = jsonObject.getJsonArray("results");

            for (int j = 0; j < jsonResults.size(); j++) {
                JsonObject jsonResult = jsonResults.getJsonObject(j);
                BasicWebContent.Builder builder = new BasicWebContent.Builder();
                if (jsonResult.get("kwic") != null) {
                    builder.setSummary(jsonResult.getString("kwic"));
                }
                builder.setUrl(jsonResult.getString("url"));
                builder.setTitle(jsonResult.getString("title"));
                builder.setSource(getName());
                webResults.add(builder.create());
                if (webResults.size() >= resultCount) {
                    break;
                }
            }

        } catch (JsonException e) {
            throw new SearcherException("Error parsing the JSON response while searching for \"" + query + "\" with "
                    + getName() + ": " + e.getMessage() + ", JSON \"" + jsonString + "\"", e);
        }

        return webResults;
    }

    // protected abstract String getRequestUrl(String query, int resultCount, Language language);

    private String getRequestUrl(String query, int resultCount, Language language) throws SearcherException {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://www.faroo.com/instant.json");
        urlBuilder.append("?q=").append(UrlHelper.encodeParameter(query));
        urlBuilder.append("&start=1");
        urlBuilder.append("&key=").append(key);
        urlBuilder.append("&length=").append(resultCount);
        urlBuilder.append("&l=");
        switch (language) {
            case ENGLISH:
                urlBuilder.append("en");
                break;
            case GERMAN:
                urlBuilder.append("de");
                break;
            case CHINESE:
                urlBuilder.append("zh");
                break;
            default:
                throw new SearcherException("Language " + language
                        + " is not supported, allowed are ENGLISH, GERMAN, CHINESE.");
        }
        urlBuilder.append("&src=").append(getSrcType());
        return urlBuilder.toString();
    }

    protected abstract String getSrcType();

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
