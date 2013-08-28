package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * {@link WebSearcher} implementation for blekko.
 * </p>
 * 
 * <p>
 * Note: blekko also works without an API key and seems to return different results depending on whether an api key is
 * set or not (as of June 30, 2012).
 * </p>
 * 
 * @see <a href="http://blekko.com">blekko</a>
 * @see <a href="http://help.blekko.com/index.php/tag/api/">API information</a>
 * @author Philipp Katz
 */
public final class BlekkoSearcher extends WebSearcher<WebResult> {

    /** Key of the {@link Configuration} key for the API key. */
    public static final String CONFIG_API_KEY = "api.blekko.key";

    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    /** The time in milliseconds we wait between two requests. */
    private static final int THROTTLING_INTERVAL_MS = 1000;

    /** Throttle the requests; this applies to all instances of the searcher. */
    private static final RequestThrottle THROTTLE = new RequestThrottle(THROTTLING_INTERVAL_MS);

    private final String apiKey;

    /**
     * <p>
     * Creates a new blekko searcher.
     * </p>
     * 
     * @param apiKey The API key for accessing blekko.
     */
    public BlekkoSearcher(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * <p>
     * Creates a new blekko searcher WITHOUT api key.
     * </p>
     * 
     */
    public BlekkoSearcher() {
        this.apiKey = null;
    }

    /**
     * <p>
     * Creates a new blekko searcher.
     * </p>
     * 
     * @param configuration The configuration which must provide an API key for accessing blekko, which must be provided
     *            as string via key <tt>api.blekko.key</tt> in the configuration.
     */
    public BlekkoSearcher(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    @Override
    public List<WebResult> search(String query, int resultCount, Language language) throws SearcherException {

        List<WebResult> webResults = new ArrayList<WebResult>();
        int pageSize = Math.min(resultCount, 100);
        int necessaryPages = (int)Math.ceil(resultCount / 100.);

        String jsonString = null;

        try {

            for (int i = 0; i < necessaryPages; i++) {

                String requestUrl = getRequestUrl(query, pageSize, i);
                THROTTLE.hold();
                HttpResult httpResult = retriever.httpGet(requestUrl);
                TOTAL_REQUEST_COUNT.incrementAndGet();

                jsonString = httpResult.getStringContent();
                JSONObject jsonObject = new JSONObject(jsonString);

                if (!jsonObject.has("RESULT")) {
                    continue;
                }

                JSONArray jsonResults = jsonObject.getJSONArray("RESULT");

                for (int j = 0; j < jsonResults.length(); j++) {
                    JSONObject jsonResult = jsonResults.getJSONObject(j);
                    String summary = null;
                    if (jsonResult.has("snippet")) {
                        summary = jsonResult.getString("snippet");
                    }
                    String url = jsonResult.getString("url");
                    String title = jsonResult.getString("url_title");
                    WebResult webResult = new WebResult(url, title, summary, getName());
                    webResults.add(webResult);
                    if (webResults.size() >= resultCount) {
                        break;
                    }
                }

            }

        } catch (HttpException e) {
            throw new SearcherException("HTTP error while searching for \"" + query + "\" with " + getName() + ": "
                    + e.getMessage(), e);
        } catch (JSONException e) {
            throw new SearcherException("Error parsing the JSON response while searching for \"" + query + "\" with "
                    + getName() + ": " + e.getMessage() + ", JSON was \"" + jsonString + "\"", e);
        }

        return webResults;
    }

    private String getRequestUrl(String query, int pageSize, int page) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://blekko.com/ws/");
        urlBuilder.append("?q=").append(UrlHelper.encodeParameter(query));
        urlBuilder.append("+/json");
        urlBuilder.append("+/ps=").append(pageSize);
        if (this.apiKey != null) {
            urlBuilder.append("&auth=").append(apiKey);
        }
        urlBuilder.append("&p=").append(page);

        // System.out.println(urlBuilder.toString());

        return urlBuilder.toString();
    }

    @Override
    public String getName() {
        return "Blekko";
    }

    @Override
    public long getTotalResultCount(String query, Language language) throws SearcherException {
        long totalResults = 0;

        String requestUrl = getRequestUrl(query, 1, 0);
        THROTTLE.hold();
        HttpResult httpResult;
        String jsonString = null;
        try {
            httpResult = retriever.httpGet(requestUrl);
            TOTAL_REQUEST_COUNT.incrementAndGet();

            jsonString = httpResult.getStringContent();
            JSONObject jsonObject = new JSONObject(jsonString);

            // System.out.println(jsonObject.toString(2));

            if (jsonObject != null && jsonObject.has("universal_total_results")) {
                String string = jsonObject.getString("universal_total_results");
                string = string.replace("K", "000");
                string = string.replace("M", "000000");
                try {
                    totalResults = Long.parseLong(string);
                } catch (Exception e) {
                    // ccl pattern in action
                }
            }

        } catch (HttpException e) {
            throw new SearcherException("HTTP error while searching total result count for \"" + query + "\" with "
                    + getName() + ": " + e.getMessage(), e);
        } catch (JSONException e) {
            throw new SearcherException("Error parsing the JSON response while searching total result count for \""
                    + query + "\" with " + getName() + ": " + e.getMessage() + ", JSON was \"" + jsonString + "\".", e);
        }

        return totalResults;
    }

    /**
     * <p>
     * Gets the number of HTTP requests sent to blekko.
     * </p>
     * 
     * @return
     */
    public static int getRequestCount() {
        return TOTAL_REQUEST_COUNT.intValue();
    }

    public static void main(String[] args) throws SearcherException {
        // CollectionHelper.print(new BlekkoSearcher(ConfigHolder.getInstance().getConfig()).search("cinefreaks", 10));
        // System.out.println(new
        // BlekkoSearcher(ConfigHolder.getInstance().getConfig()).getTotalResultCount("inurl:\"cinefreaks\""));
    }
}
