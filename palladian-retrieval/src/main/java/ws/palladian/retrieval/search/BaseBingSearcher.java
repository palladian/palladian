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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.resources.BasicWebContent;
import ws.palladian.retrieval.resources.WebContent;

/**
 * <p>
 * Base implementation for Bing searchers.
 * </p>
 * 
 * @see <a href="https://datamarket.azure.com/dataset/bing/search">Bing Search API on Windows Azure Marketplace</a>
 * @author Philipp Katz
 */
public abstract class BaseBingSearcher<R extends WebContent> extends AbstractMultifacetSearcher<R> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseBingSearcher.class);

    /** The base URL endpoint of the Bing service. */
    private static final String BASE_SERVICE_URL = "https://api.datamarket.azure.com/Bing/Search/Composite";

    /** Key of the {@link Configuration} key for the account key. */
    public static final String CONFIG_ACCOUNT_KEY = "api.bing.accountkey";

    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    protected final String accountKey;
    
    private final HttpRetriever retriever;

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
        this.retriever = HttpRetrieverFactory.getHttpRetriever();
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
    public SearchResults<R> search(MultifacetQuery query) throws SearcherException {

        List<R> webResults = new ArrayList<R>();

        int necessaryPages = (int)Math.ceil((double)query.getResultCount() / getDefaultFetchSize());
        int offset = 0;
        Long totalResults = null;

        for (int i = 0; i < necessaryPages; i++) {

            String requestUrl = buildRequestUrl(query.getText(), query.getLanguage(), offset, getDefaultFetchSize());
            LOGGER.debug("Requesting {}", requestUrl);
            String jsonString = null;

            try {
                jsonString = getResponseData(requestUrl);
                JsonObject responseData = new JsonObject(jsonString).getJsonObject("d");
                TOTAL_REQUEST_COUNT.incrementAndGet();

                long currentTotal = responseData.queryLong("results[0]/" + getSourceType() + "Total");
                if (totalResults == null) {
                    // get it on first page, values on follow up pages do not seem trustworthy; e.g. for a query "obama",
                    // I get a value of 90800000 for total results; which later shrinks to 767. Seems, that Bing does not
                    // want to give all results here?
                    totalResults = currentTotal;
                }
                offset += getDefaultFetchSize();

                JsonArray results = responseData.queryJsonArray("results[0]/" + getSourceType());
                for (int j = 0; j < results.size(); j++) {
                    JsonObject currentResult = results.getJsonObject(j);
                    R webResult = parseResult(currentResult);
                    webResults.add(webResult);
                    if (webResults.size() >= query.getResultCount()) {
                        break;
                    }
                }
                
                if (webResults.size() >= currentTotal) {
                    // no more results
                    break;
                }

            } catch (HttpException e) {
                throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName() + ": "
                        + e.getMessage(), e);
            } catch (JsonException e) {
                throw new SearcherException("Error parsing the JSON response while searching for \"" + query
                        + "\" with " + getName() + ": " + e.getMessage() + ", url: \"" + requestUrl + "\", json: \""
                        + jsonString + "\"", e);
            }
        }
        
        return new SearchResults<R>(webResults, totalResults);
    }

    /**
     * <p>
     * Parse the {@link JSONObject} to the desired type of {@link BasicWebContent}.
     * </p>
     * 
     * @param currentResult The JSON entry to parse.
     * @return A parsed entry of type {@link WebContent} or subclass.
     * @throws JsonException In case the JSON could not be parsed.
     */
    protected abstract R parseResult(JsonObject currentResult) throws JsonException;

    /**
     * @return The String description for this source, i.e. Web, Image, or News.
     */
    protected abstract String getSourceType();

    /**
     * @return Return the default fetch size, i.e. the number of results being fetched with each request.
     */
    protected abstract int getDefaultFetchSize();

    /**
     * <p>
     * Perform the HTTP request and return the JSON result string.
     * </p>
     * 
     * @param requestUrl The URL to request.
     * @return The response as String.
     * @throws HttpException In case of a network exception.
     * @throws SearcherException In case of an error HTTP status code.
     */
    private String getResponseData(String requestUrl) throws HttpException, SearcherException {
        String basicAuthentication = "Basic " + StringHelper.encodeBase64(":" + accountKey);
        HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, requestUrl);
        httpRequest.addHeader("Authorization", basicAuthentication);
        HttpResult httpResult = retriever.execute(httpRequest);
        if (httpResult.errorStatus()) {
            throw new SearcherException("Encountered HTTP status " + httpResult.getStatusCode() + ": "
                    + httpResult.getStringContent());
        }
        return httpResult.getStringContent();
    }

    /**
     * <p>
     * Build a search request URL based on the supplied parameters.
     * </p>
     * 
     * @param query the raw query, no escaping necessary.
     * @param language the language for which to search, may be <code>null</code>.
     * @param offset the paging offset, 0 for no offset.
     * @param count the number of results to retrieve.
     * @return The URL for the API request.
     */
    protected String buildRequestUrl(String query, Language language, int offset, int count) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(getBaseServiceUrl());
        queryBuilder.append("?Sources=%27").append(getSourceType()).append("%27");
        queryBuilder.append("&Query=%27").append(UrlHelper.encodeParameter(query)).append("%27");
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
     * @return Get the base service URL (must be the "composite" endpoint, because only this supports getting the total
     *         number of available results).
     */
    protected String getBaseServiceUrl() {
        return BASE_SERVICE_URL;
    }

    /**
     * <p>
     * Transform the {@link Language} into a string identifier. See <a
     * href="http://msdn.microsoft.com/en-us/library/dd251064.aspx">here</a> available language codes.
     * </p>
     * 
     * @param language The language to map.
     * @return Language string identifier for the given language.
     */
    private String getLanguageString(Language language) {
        switch (language) {
            case GERMAN:
                return "de-de";
            default:
                break;
        }
        return "en-us";
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
            LOGGER.trace("Error parsing date " + dateString, e);
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
