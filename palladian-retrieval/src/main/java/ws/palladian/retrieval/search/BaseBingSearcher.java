package ws.palladian.retrieval.search;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.search.web.WebResult;
import ws.palladian.retrieval.search.web.WebSearcher;

/**
 * <p>
 * Base implementation for Bing searchers.
 * </p>
 * 
 * @see <a href="https://datamarket.azure.com/dataset/bing/search">Bing Search API on Windows Azure Marketplace</a>
 * @author Philipp Katz
 */
public abstract class BaseBingSearcher<R extends WebResult> extends WebSearcher<R> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseBingSearcher.class);

    /** The base URL endpoint of the Bing service. */
    private static final String BASE_SERVICE_URL = "https://api.datamarket.azure.com/Bing/Search/v1/";

    /** Key of the {@link Configuration} key for the account key. */
    public static final String CONFIG_ACCOUNT_KEY = "api.bing.accountkey";

    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    protected final String accountKey;

    /**
     * <p>
     * Creates a new Bing searcher.
     * </p>
     * 
     * @param accountKey The account key for accessing Bing.
     */
    public BaseBingSearcher(String accountKey) {
        Validate.notEmpty(accountKey, "accountKey must not be empty");
        this.accountKey = accountKey;
    }

    /**
     * <p>
     * Creates a new Bing searcher.
     * </p>
     * 
     * @param configuration The configuration which must provide an account key for accessing Bing, which must be
     *            provided as string via key <tt>api.bing.key</tt> in the configuration.
     */
    public BaseBingSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_ACCOUNT_KEY));
    }

    @Override
    public List<R> search(String query, int resultCount, Language language) throws SearcherException {

        List<R> webResults = new ArrayList<R>();

        int necessaryPages = (int)Math.ceil((double)resultCount / getDefaultFetchSize());
        int offset = 0;

        for (int i = 0; i < necessaryPages; i++) {

            String sourceType = getSourceType();
            String requestUrl = buildRequestUrl(query, sourceType, language, offset, getDefaultFetchSize());
            String jsonString = null;

            try {

                jsonString = getResponseData(requestUrl, accountKey);

                JSONObject jsonObject = new JSONObject(jsonString);
                JSONObject responseData = jsonObject.getJSONObject("d");
                TOTAL_REQUEST_COUNT.incrementAndGet();

                JSONArray results = responseData.getJSONArray("results");
                int numResults = results.length();
                offset += numResults;

                for (int j = 0; j < numResults; j++) {
                    JSONObject currentResult = results.getJSONObject(j);
                    R webResult = parseResult(currentResult);
                    webResults.add(webResult);

                    if (webResults.size() >= resultCount) {
                        break;
                    }
                }

                if (!responseData.has("__next")) {
                    break;
                }

            } catch (HttpException e) {
                throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName() + ": "
                        + e.getMessage(), e);
            } catch (JSONException e) {
                throw new SearcherException("Error parsing the JSON response while searching for \"" + query
                        + "\" with " + getName() + ": " + e.getMessage() + ", url: \"" + requestUrl + "\", json: \""
                        + jsonString + "\"", e);
            }
        }

        LOGGER.debug("bing requests: " + TOTAL_REQUEST_COUNT.get());

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
     * Return the String description for this source, i.e. Web, Image, or News.
     * </p>
     * 
     * @return
     */
    protected abstract String getSourceType();

    /**
     * <p>
     * Return the default fetch size, i.e. the number of results being fetched with each request.
     * </p>
     * 
     * @return
     */
    protected abstract int getDefaultFetchSize();

    /**
     * <p>
     * Perform the HTTP request and return the JSON result string.
     * </p>
     * 
     * @param requestUrl
     * @return
     * @throws HttpException
     * @throws JSONException
     */
    private String getResponseData(String requestUrl, String accountKey) throws HttpException, JSONException {
        String basicAuthentication = "Basic " + StringHelper.encodeBase64(":" + accountKey);
        HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, requestUrl);
        httpRequest.addHeader("Authorization", basicAuthentication);
        HttpResult httpResult = retriever.execute(httpRequest);
        String jsonString = new String(HttpHelper.getStringContent(httpResult));
        return jsonString;
    }

    /**
     * <p>
     * Build a search request URL based on the supplied parameters.
     * </p>
     * 
     * @param query the raw query, no escaping necessary.
     * @param sourceType type of source to query, i.e. Web, Image, or News.
     * @param language the language for which to search, may be <code>null</code>.
     * @param offset the paging offset, 0 for no offset.
     * @param count the number of results to retrieve.
     * @return
     */
    protected String buildRequestUrl(String query, String sourceType, Language language, int offset, int count) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(BASE_SERVICE_URL);
        queryBuilder.append(sourceType);
        queryBuilder.append("?Query=%27").append(UrlHelper.encodeParameter(query)).append("%27");
        queryBuilder.append("&$top=").append(count);
        if (offset > 0) {
            queryBuilder.append("&$skip=").append(offset);
        }
        queryBuilder.append("&$format=JSON");
        if (language != null) {
            queryBuilder.append("&Market=%27").append(getLanguageString(language)).append("%27");
        }
        return queryBuilder.toString();
    }

    /**
     * <p>
     * Transform the {@link Language} into a string identifier. See <a
     * href="http://msdn.microsoft.com/en-us/library/dd251064.aspx">here</a> available language codes.
     * </p>
     * 
     * @param language
     * @return
     */
    protected String getLanguageString(Language language) {
        switch (language) {
            case GERMAN:
                return "de-de";
            default:
                break;
        }
        return "en-us";
    }

    @Override
    public long getTotalResultCount(String query, Language language) throws SearcherException {
        throw new SearcherException("Getting the total result count is not supported in the new Bing API.");
    }

    /**
     * <p>
     * Parses the supplied string to a date.
     * </p>
     * 
     * @param dateString
     * @return the date, or <code>null</code> if the string could not be parsed.
     */
    protected Date parseDate(String dateString) {
        Date result = null;
        DateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
        try {
            result = dateFormat.parse(dateString);
        } catch (ParseException e) {
            LOGGER.trace("error parsing date " + dateString, e);
        }
        return result;
    }

    /**
     * Gets the number of HTTP requests sent to Bing.
     * 
     * @return
     */
    public static int getRequestCount() {
        return TOTAL_REQUEST_COUNT.get();
    }

}
