package ws.palladian.retrieval.search.services;

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
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.search.Searcher;
import ws.palladian.retrieval.search.WebImageResult;
import ws.palladian.retrieval.search.WebResult;

/**
 * <p>
 * Base implementation for Bing searchers.
 * </p>
 * 
 * @see http://www.bing.com/developers/s/APIBasics.html
 * @author Philipp Katz
 */
public abstract class BaseBingSearcher<R extends WebResult> extends BaseWebSearcher<R> {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(BaseBingSearcher.class);

    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static final AtomicInteger requestCount = new AtomicInteger();

    protected final String apiKey;

    public BaseBingSearcher(String apiKey) {
        super();
        this.apiKey = apiKey;
    }

    public BaseBingSearcher() {
        ConfigHolder configHolder = ConfigHolder.getInstance();
        PropertiesConfiguration config = configHolder.getConfig();
        apiKey = config.getString("api.bing.key");
    }

    @Override
    public List<R> search(String query) {

        List<R> webResults = new ArrayList<R>();

        int necessaryPages = (int) Math.ceil((double) getResultCount() / getDefaultFetchSize());
        int offset = 0;

        try {

            for (int i = 0; i < necessaryPages; i++) {

                String sourceType = getSourceType();
                String requestUrl = getRequestUrl(query, sourceType, getLanguage(), offset, getDefaultFetchSize());
                JSONObject responseData = getResponseData(requestUrl, sourceType);
                requestCount.incrementAndGet();

                int total = responseData.getInt("Total");
                if (offset > total) {
                    break;
                }

                JSONArray results = responseData.getJSONArray("Results");
                int numResults = results.length();
                offset += numResults;

                for (int j = 0; j < numResults; j++) {
                    JSONObject currentResult = results.getJSONObject(j);
                    R webResult = parseResult(currentResult);
                    webResults.add(webResult);

                    if (webResults.size() >= getResultCount()) {
                        break;
                    }
                }
            }
        } catch (HttpException e) {
            LOGGER.error(e);
        } catch (JSONException e) {
            LOGGER.error(e);
        }

        LOGGER.debug("bing requests: " + requestCount.get());
        return webResults;

    }

    /**
     * Parse the {@link JSONObject} to the desired type of {@link WebResult}.
     * 
     * @param currentResult
     * @return
     * @throws JSONException
     */
    protected abstract R parseResult(JSONObject currentResult) throws JSONException;

    /**
     * Return the String description for this source, i.e. Web, Image, or News.
     * 
     * @return
     */
    protected abstract String getSourceType();

    /**
     * Return the default fetch size, i.e. the number of results being fetched with each request.
     * 
     * @return
     */
    protected abstract int getDefaultFetchSize();

    private JSONObject getResponseData(String requestUrl, String sourceType) throws HttpException, JSONException {
        HttpResult httpResult = retriever.httpGet(requestUrl);
        String jsonString = new String(httpResult.getContent());
        JSONObject jsonObject = new JSONObject(jsonString);
        JSONObject responseData = jsonObject.getJSONObject("SearchResponse").getJSONObject(sourceType);
        return responseData;
    }

    /**
     * 
     * @param query
     * @param sourceType type of source to query, i.e. Web, Image, or News.
     * @param language
     * @param offset
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

    protected String getLanguageString(WebSearcherLanguage language) {
        switch (language) {
            case GERMAN:
                return "de-de";
        }
        return "en-us";
    }

    @Override
    public int getResultCount(String query) {
        int hitCount = 0;
        try {
            String sourceType = getSourceType();
            String requestUrl = getRequestUrl(query, sourceType, getLanguage(), 0, 1);
            JSONObject responseData = getResponseData(requestUrl, sourceType);
            hitCount = responseData.getInt("Total");
        } catch (HttpException e) {
            LOGGER.error(e);
        } catch (JSONException e) {
            LOGGER.error(e);
        }
        return hitCount;
    }

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

    public static void main(String[] args) throws ParseException {
        // DateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
        // System.out.println(dateFormat.format(new Date()));
        // Date date = dateFormat.parse("2011-11-24T06:46:00Z");
        // System.out.println(date);
        // System.exit(0);

        // Searcher<WebResult> searcher = new BingNewsSearcher();
        Searcher<WebImageResult> searcher = new BingImageSearcher();
        searcher.setResultCount(10);
        List<WebImageResult> result = searcher.search("apple");
        CollectionHelper.print(result);
        System.out.println(BaseBingSearcher.requestCount);
    }

}
