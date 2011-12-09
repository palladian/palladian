package ws.palladian.retrieval.search.web;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;

/**
 * <p>
 * Base implementation for Bing searchers.
 * </p>
 * 
 * @see http://www.bing.com/developers/s/APIBasics.html
 * @author Philipp Katz
 */
public abstract class BaseBingSearcher<R extends WebResult> extends WebSearcher<R> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(BaseBingSearcher.class);

    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    protected final String apiKey;

    /**
     * <p>
     * Creates a new Bing searcher.
     * </p>
     * 
     * @param apiKey The API key for accessing Bing.
     */
    public BaseBingSearcher(String apiKey) {
        super();
        if (apiKey == null) {
            throw new IllegalStateException("The required API key is missing");
        }
        this.apiKey = apiKey;
    }

    /**
     * <p>
     * Creates a new Bing searcher.
     * </p>
     * 
     * @param configuration The configuration which must provide an API key for accessing Bing, which must be provided
     *            as string via key <tt>api.bing.key</tt> in the configuration.
     */
    public BaseBingSearcher(PropertiesConfiguration configuration) {
        this(configuration.getString("api.bing.key"));
    }

    /**
     * <p>
     * Creates a new Bing searcher. The necessary API key is taken from the global configuration registry, which must
     * provide the API key as string via key <tt>api.bing.key</tt>.
     * </p>
     * 
     * @see ConfigHolder
     */
    public BaseBingSearcher() {
        this(ConfigHolder.getInstance().getConfig());
    }

    @Override
    public List<R> search(String query, int resultCount, WebSearcherLanguage language) {

        List<R> webResults = new ArrayList<R>();

        int necessaryPages = (int) Math.ceil((double) resultCount / getDefaultFetchSize());
        int offset = 0;

        try {

            for (int i = 0; i < necessaryPages; i++) {

                String sourceType = getSourceType();
                String requestUrl = getRequestUrl(query, sourceType, language, offset, getDefaultFetchSize());
                JSONObject responseData = getResponseData(requestUrl, sourceType);
                TOTAL_REQUEST_COUNT.incrementAndGet();

                int total = responseData.getInt("Total");
                if (total == 0) {
                    break;
                }

                JSONArray results = responseData.getJSONArray("Results");
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

                if (offset >= total) {
                    break;
                }

            }
        } catch (HttpException e) {
            LOGGER.error(e);
        } catch (JSONException e) {
            LOGGER.error(e);
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
     * Perform the HTTP request and return the relevant JSON result.
     * </p>
     * 
     * @param requestUrl
     * @param sourceType
     * @return
     * @throws HttpException
     * @throws JSONException
     */
    private JSONObject getResponseData(String requestUrl, String sourceType) throws HttpException, JSONException {
        HttpResult httpResult = retriever.httpGet(requestUrl);
        String jsonString = new String(httpResult.getContent());
        System.out.println(jsonString);
        JSONObject jsonObject = new JSONObject(jsonString);
        JSONObject responseData = jsonObject.getJSONObject("SearchResponse").getJSONObject(sourceType);
        return responseData;
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
    protected String getRequestUrl(String query, String sourceType, WebSearcherLanguage language, int offset, int count) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("http://api.bing.net/json.aspx");
        queryBuilder.append("?AppId=").append(apiKey);
        queryBuilder.append("&").append(sourceType).append(".Count=").append(count);
        if (offset > 0) {
            queryBuilder.append("&").append(sourceType).append(".Offset=").append(offset);
        }
        queryBuilder.append("&Sources=").append(sourceType);
        queryBuilder.append("&JsonType=raw");
        queryBuilder.append("&Adult=Moderate");
        if (language != null) {
            queryBuilder.append("&Market=").append(getLanguageString(language));
        }
        queryBuilder.append("&Query=").append(UrlHelper.urlEncode(query));
        return queryBuilder.toString();
    }

    /**
     * <p>
     * Transform the {@link WebSearcherLanguage} into a string identifier. See Bing API documentation for available
     * language codes.
     * </p>
     * 
     * @param language
     * @return
     */
    protected String getLanguageString(WebSearcherLanguage language) {
        switch (language) {
            case GERMAN:
                return "de-de";
        }
        return "en-us";
    }

    @Override
    public int getTotalResultCount(String query, WebSearcherLanguage language) {
        int hitCount = 0;
        try {
            String sourceType = getSourceType();
            String requestUrl = getRequestUrl(query, sourceType, language, 0, 1);
            JSONObject responseData = getResponseData(requestUrl, sourceType);
            hitCount = responseData.getInt("Total");
        } catch (HttpException e) {
            LOGGER.error(e);
        } catch (JSONException e) {
            LOGGER.error(e);
        }
        return hitCount;
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
