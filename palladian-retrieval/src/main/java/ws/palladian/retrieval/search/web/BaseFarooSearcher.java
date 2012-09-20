package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * {@link WebSearcher} implementation for faroo.
 * </p>
 * 
 * <p>
 * Rate limit is 100,000 queries per month.
 * </p>
 * 
 * @see http://www.faroo.com/
 * @see http://www.faroo.com/hp/api/api.html#jsonp
 * @author David Urbansky
 */
abstract class BaseFarooSearcher extends WebSearcher<WebResult> {

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    /**
     * <p>
     * Creates a new faroo searcher.
     * </p>
     * 
     */
    public BaseFarooSearcher() {
        super();
    }

    /**
     * <p>
     * Supported languages are English, German, and Chinese.
     * </p>
     */
    @Override
    public List<WebResult> search(String query, int resultCount, Language language) throws SearcherException {

        List<WebResult> webResults = new ArrayList<WebResult>();

        try {
            String requestUrl = getRequestUrl(query, resultCount, language);
                HttpResult httpResult = retriever.httpGet(requestUrl);
                TOTAL_REQUEST_COUNT.incrementAndGet();

                String jsonString = HttpHelper.getStringContent(httpResult);
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
                    WebResult webResult = new WebResult(url, title, summary, getName());
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
