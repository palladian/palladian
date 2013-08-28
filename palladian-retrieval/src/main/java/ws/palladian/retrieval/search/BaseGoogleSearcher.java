package ws.palladian.retrieval.search;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.search.web.WebResult;
import ws.palladian.retrieval.search.web.WebSearcher;

/**
 * <p>
 * Base implementation for all Google searchers. Subclasses must implement {@link #getBaseUrl()}, which provides the URL
 * to the API endpoint and {@link #parseResult(JSONObject)}, which is responsible for parsing the JSONObject for each
 * result to the desired type ({@link WebResult} or subclasses).
 * </p>
 * 
 * @see http://code.google.com/intl/de/apis/websearch/docs/reference.html
 * @author Philipp Katz
 */
public abstract class BaseGoogleSearcher<R extends WebResult> extends WebSearcher<R> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseGoogleSearcher.class);

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    @Override
    public List<R> search(String query, int resultCount, Language language) throws SearcherException {

        List<R> webResults = new ArrayList<R>();

        // the number of pages we need to check; each page returns 8 results
        int necessaryPages = (int)Math.ceil(resultCount / 8.);

        for (int i = 0; i < necessaryPages; i++) {

            int offset = i * 8;
            String responseString = getResponseData(query, language, offset);

            try {

                JSONObject jsonObject = new JSONObject(responseString);
                JSONObject responseData = jsonObject.getJSONObject("responseData");

                TOTAL_REQUEST_COUNT.incrementAndGet();

                // in the first iteration find the maximum of available pages and limit the search to those
                if (i == 0) {
                    int availablePages = getAvailablePages(responseData);
                    if (necessaryPages > availablePages) {
                        necessaryPages = availablePages;
                    }
                }

                JSONArray results = responseData.getJSONArray("results");
                for (int j = 0; j < results.length(); j++) {
                    JSONObject resultJson = results.getJSONObject(j);
                    R webResult = parseResult(resultJson);
                    webResults.add(webResult);
                    if (webResults.size() >= resultCount) {
                        break;
                    }
                }
            } catch (JSONException e) {
                throw new SearcherException("Exception parsing the JSON response while searching for \"" + query
                        + "\" with " + getName() + ": " + e.getMessage() + ", JSON was: \"" + responseString + "\"", e);
            }
        }

        LOGGER.debug("google requests: " + TOTAL_REQUEST_COUNT.get());
        return webResults;
    }

    /**
     * Return the base URL for accessing this specific search.
     * 
     * @return
     */
    protected abstract String getBaseUrl();

    private String getRequestUrl(String query, Language language, int start) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(getBaseUrl());
        queryBuilder.append("?v=1.0");
        if (start > 0) {
            queryBuilder.append("&start=").append(start);
        }
        // rsz=large will respond 8 results
        queryBuilder.append("&rsz=large");
        queryBuilder.append("&safe=off");
        if (language != null) {
            queryBuilder.append("&lr=").append(getLanguageString(language));
        }
        queryBuilder.append("&q=").append(UrlHelper.encodeParameter(query));
        return queryBuilder.toString();
    }

    private String getResponseData(String query, Language language, int offset) throws SearcherException {
        String requestUrl = getRequestUrl(query, language, offset);
        HttpResult httpResult;
        try {
            httpResult = retriever.httpGet(requestUrl);
        } catch (HttpException e) {
            throw new SearcherException("HTTP exception while searching for \"" + query + "\" with " + getName() + ": "
                    + e.getMessage(), e);
        }
        return httpResult.getStringContent();
    }

    /**
     * Get the string representation for the desired language.
     * 
     * @see http://www.google.com/cse/docs/resultsxml.html#languageCollections
     * @param language
     * @return
     */
    private String getLanguageString(Language language) {
        switch (language) {
            case GERMAN:
                return "lang_de";
            default:
                break;
        }
        return "lang_en";
    }

    /**
     * Parse the number of available pages from the responseData object.
     * 
     * @param responseData
     * @return
     * @throws JSONException
     */
    private int getAvailablePages(JSONObject responseData) throws JSONException {
        int availablePages = -1;
        if (responseData.has("cursor")) {
            JSONObject cursor = responseData.getJSONObject("cursor");
            if (cursor.has("pages")) {
                JSONArray pages = cursor.getJSONArray("pages");
                availablePages = pages.length();
            }
        }
        return availablePages;
    }

    /**
     * Parse one result object from JSON to an instance of {@link WebResult}.
     * 
     * @param resultData
     * @return
     * @throws JSONException
     */
    protected abstract R parseResult(JSONObject resultData) throws JSONException;

    @Override
    public long getTotalResultCount(String query, Language language) throws SearcherException {
        long hitCount = 0;
        String responseData = getResponseData(query, null, 0);
        try {
            JSONObject responseJson = new JSONObject(responseData);
            if (responseJson.has("cursor")) {
                JSONObject cursor = responseJson.getJSONObject("cursor");
                if (cursor.has("estimatedResultCount")) {
                    hitCount = cursor.getLong("estimatedResultCount");
                }
            }
        } catch (JSONException e) {
            throw new SearcherException("Exception parsing the JSON response while searching for \"" + query
                    + "\" with " + getName() + ": " + e.getMessage() + ", JSON was: \"" + responseData + "\"", e);
        }
        return hitCount;
    }

    /**
     * Gets the number of HTTP requests sent to Google.
     * 
     * @return
     */
    public static int getRequestCount() {
        return TOTAL_REQUEST_COUNT.get();
    }
}
