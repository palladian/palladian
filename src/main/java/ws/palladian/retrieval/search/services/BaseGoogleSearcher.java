package ws.palladian.retrieval.search.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.search.WebResult;
import ws.palladian.retrieval.search.WebSearcherManager;

/**
 * Base implementation for all Google searches. Subclasses typically implement {@link #getBaseUrl()} and
 * {@link #parseResult(JSONObject)}.
 * 
 * @see http://code.google.com/intl/de/apis/ajaxsearch/documentation/reference.html#_property_GSearch
 * @author Philipp Katz
 */
public abstract class BaseGoogleSearcher extends BaseWebSearcher implements WebSearcher {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(BaseGoogleSearcher.class);

    private static final AtomicInteger requestCount = new AtomicInteger();

    public BaseGoogleSearcher() {
        super();
    }

    @Override
    public List<WebResult> search(String query, int resultCount, WebSearcherLanguage language) {

        List<WebResult> webResults = new ArrayList<WebResult>();

        // the number of pages we need to check; each page returns 8 results
        int necessaryPages = (int) Math.ceil(resultCount / 8.);

        try {
            for (int i = 0; i < necessaryPages; i++) {

                int offset = i * 8;
                JSONObject responseData = getResponseData(query, language, offset);
                requestCount.incrementAndGet();

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
                    WebResult webResult = parseResult(resultJson);
                    webResults.add(webResult);
                    if (webResults.size() >= resultCount) {
                        break;
                    }
                }
            }
        } catch (HttpException e) {
            LOGGER.error(e);
        } catch (JSONException e) {
            LOGGER.error(e);
        }

        LOGGER.debug("google requests: " + requestCount.get());
        return webResults;
    }

    /**
     * Return the base URL for accessing this specific search.
     * 
     * @return
     */
    protected abstract String getBaseUrl();

    private String getRequestUrl(String query, WebSearcherLanguage language, int start) {
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
        queryBuilder.append("&q=").append(UrlHelper.urlEncode(query));
        return queryBuilder.toString();
    }

    private JSONObject getResponseData(String query, WebSearcherLanguage language, int offset) throws HttpException,
            JSONException {
        String requestUrl = getRequestUrl(query, language, offset);
        HttpResult httpResult = retriever.httpGet(requestUrl);
        String jsonString = new String(httpResult.getContent());
        JSONObject jsonObject = new JSONObject(jsonString);
        if (!jsonObject.has("responseData")) {
            // TODO throw some exception
        }
        return jsonObject.getJSONObject("responseData");
    }

    /**
     * Get the string representation for the desired language.
     * 
     * @see http://www.google.com/cse/docs/resultsxml.html#languageCollections
     * @param language
     * @return
     */
    private String getLanguageString(WebSearcherLanguage language) {
        switch (language) {
            case GERMAN:
                return "lang_de";
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
        if (responseData.getJSONObject("cursor") != null) {
            JSONObject cursor = responseData.getJSONObject("cursor");
            if (cursor.getJSONArray("pages") != null) {
                JSONArray pages = cursor.getJSONArray("pages");
                availablePages = pages.length();
            }
        }
        return availablePages;
    }

    /**
     * Parse the number of estimed results from the responseData object.
     * 
     * @param responseData
     * @return
     * @throws JSONException
     */
    private int getResultCount(JSONObject responseData) throws JSONException {
        int resultCount = -1;
        if (responseData.getJSONObject("cursor") != null) {
            JSONObject cursor = responseData.getJSONObject("cursor");
            if (cursor.has("estimatedResultCount")) {
                resultCount = cursor.getInt("estimatedResultCount");
            }
        }
        return resultCount;
    }

    /**
     * Parse one result object to a {@link WebResult}.
     * 
     * @param resultData
     * @return
     * @throws JSONException
     */
    protected WebResult parseResult(JSONObject resultData) throws JSONException {
        String title = resultData.getString("titleNoFormatting");
        String content = resultData.getString("content");
        String url = resultData.getString("unescapedUrl");
        WebResult webResult = new WebResult(WebSearcherManager.GOOGLE, 0, url, title, content);
        return webResult;
    }

    @Override
    public int getResultCount(String query) {
        int hitCount = 0;
        try {
            JSONObject responseData = getResponseData(query, null, 0);
            hitCount = getResultCount(responseData);
        } catch (HttpException e) {
            LOGGER.error(e);
        } catch (JSONException e) {
            LOGGER.error(e);
        }
        return hitCount;
    }

    public static void main(String[] args) {
        // WebSearcher searcher = new GoogleBlogsSearcher();
        WebSearcher searcher = new GoogleImageSearcher();
        List<WebResult> queryResult = searcher.search("apple");
        CollectionHelper.print(queryResult);

    }
}
