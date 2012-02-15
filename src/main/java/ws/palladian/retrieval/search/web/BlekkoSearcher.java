package ws.palladian.retrieval.search.web;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;

/**
 * <p>
 * {@link WebSearcher} implementation for blekko.
 * </p>
 * 
 * @see http://blekko.com
 * @see http://help.blekko.com/index.php/tag/api/
 * @author Philipp Katz
 */
public final class BlekkoSearcher extends WebSearcher<WebResult> {


    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(BlekkoSearcher.class);

    /** Key of the {@link Configuration} key for the API key. */
    public static final String CONFIG_API_KEY = "api.blekko.key";
    
    private static final AtomicInteger TOTAL_REQUEST_COUNT = new AtomicInteger();

    /** The time in milliseconds we wait between two requests. */
    private static final int THROTTLING_INTERVAL_MS = 1000;

    /** The timestamp of the last request. */
    private static Long lastRequestTimestamp;

    private final String apiKey;

    /**
     * <p>
     * Creates a new blekko searcher.
     * </p>
     * 
     * @param apiKey The API key for accessing blekko.
     */
    public BlekkoSearcher(String apiKey) {
        super();
        this.apiKey = apiKey;
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
    public List<WebResult> search(String query, int resultCount, WebSearcherLanguage language) {

        List<WebResult> webResults = new ArrayList<WebResult>();
        int pageSize = Math.min(resultCount, 100);
        int necessaryPages = (int) Math.ceil(resultCount / 100.);

        try {

            for (int i = 0; i < necessaryPages; i++) {

                String requestUrl = getRequestUrl(query, pageSize, i);
                checkQueryThrottling();
                HttpResult httpResult = retriever.httpGet(requestUrl);
                TOTAL_REQUEST_COUNT.incrementAndGet();

                String jsonString = new String(httpResult.getContent());
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
                    WebResult webResult = new WebResult(url, title, summary);
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

        return webResults;
    }

    private String getRequestUrl(String query, int pageSize, int page) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://blekko.com/ws/");
        urlBuilder.append("?q=").append(UrlHelper.urlEncode(query));
        urlBuilder.append("+/json");
        urlBuilder.append("+/ps=").append(pageSize);
        urlBuilder.append("&auth=").append(apiKey);
        urlBuilder.append("&p=").append(page);
        return urlBuilder.toString();
    }

    /**
     * <p>
     * Make sure, we consider the one-second limit between successive requests. If we are below one second, this method
     * blocks and waits.
     * </p>
     */
    private static synchronized void checkQueryThrottling() {
        if (lastRequestTimestamp != null) {
            long millisSinceLastRequest = System.currentTimeMillis() - lastRequestTimestamp;
            if (millisSinceLastRequest < THROTTLING_INTERVAL_MS) {
                try {
                    long millisToSleep = THROTTLING_INTERVAL_MS - millisSinceLastRequest;
                    Thread.sleep(millisToSleep);
                } catch (InterruptedException e) {
                    LOGGER.error(e);
                }
            }
        }
        lastRequestTimestamp = System.currentTimeMillis();
    }

    @Override
    public String getName() {
        return "Blekko";
    }

    /**
     * Gets the number of HTTP requests sent to blekko.
     * 
     * @return
     */
    public static int getRequestCount() {
        return TOTAL_REQUEST_COUNT.intValue();
    }

}
